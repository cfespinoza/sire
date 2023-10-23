import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, HttpApp, RejectionHandler, Route}
import akka.stream.Materializer
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import akka.Done
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.corsRejectionHandler
import com.typesafe.config.ConfigFactory

import java.io.{File, PrintWriter}
import scala.io.StdIn
import scala.sys.process._
import scala.util.{Failure, Success, Try}

object Server extends HttpApp with App {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "sire-server")
  implicit val executionContext = system.executionContext
  implicit val materializer: Materializer = Materializer(system)

  val config = ConfigFactory.load()

  // formats for unmarshalling and marshalling
  implicit val itemFormat: RootJsonFormat[RowData] = jsonFormat7(RowData.apply)
  implicit val orderFormat: RootJsonFormat[Data] = jsonFormat4(Data.apply)

  final case class RowData(id: String,
                           customer_focus: List[String],
                           sectors: List[String],
                           date: Long,
                           revenue: Double,
                           yoy_growth: Double,
                           next_yoy_growth: Double)

  final case class Data(company_id: String
                        , latest_known_dt: String
                        , extrapolate_len: Option[Int]
                        , data: List[RowData])

  //  val route: Route =
  private val serverHost = config.getString("sireserver.server-host")
  private val serverPort = config.getInt("sireserver.server-port")
  println(s"Server online at http://${serverHost}:${serverPort}/\nPress RETURN to stop...")

  //  private val bindingFuture = Http().newServerAt(serverHost, serverPort).bind(route)
  startServer(serverHost, serverPort)

  override protected def routes: Route = {
    import ch.megard.akka.http.cors.scaladsl.CorsDirectives._

    // Your CORS settings are loaded from `application.conf`

    // Your rejection handler
    val rejectionHandler = corsRejectionHandler.withFallback(RejectionHandler.default)

    // Your exception handler
    val exceptionHandler = ExceptionHandler { case e: NoSuchElementException =>
      complete(StatusCodes.NotFound -> e.getMessage)
    }

    // Combining the two handlers only for convenience
    val handleErrors = handleRejections(rejectionHandler) & handleExceptions(exceptionHandler)
    handleErrors {
      cors() {
        handleErrors {
          concat(
            get {
              pathSingleSlash {
                complete(
                  HttpEntity(
                    ContentTypes.`text/html(UTF-8)`,
                    Constants.htmlContent
                ))
              }
            },
            get {
              path("health") {
                complete("The server is up and running!")
              }
            },
            post {
              path("predict") {
                entity(as[Data]) { data => {
                  val tempFile = Try(File.createTempFile("tempFile", ".json"))
                  tempFile match {
                    case Failure(_) =>
                      complete(StatusCodes.InternalServerError, "Failed to create temp file")
                    case Success(file) =>
                      // Escribir el JSON en el archivo temporal
                      val writer = new PrintWriter(file)
                      writer.write(orderFormat.write(data).toString())
                      writer.close()
                      val executable = config.getString("sireserver.executable")
                      val scriptPath = config.getString("sireserver.scriptPath")
                      val databaseFilePath = config.getString("sireserver.databaseFilePath")
                      val command = s"$executable $scriptPath -j ${file.getPath} -d $databaseFilePath -o ${file.getPath}"

                      // Invocar el proceso python con el archivo temporal como argumento
                      val output = Try(command.!!)
                      val outputContent: String = scala.io.Source.fromFile(file).mkString
                      file.delete()

                      output match {
                        case Failure(exception) =>
                          complete(StatusCodes.InternalServerError, s"Python script execution failed: ${exception.getMessage}")
                        case Success(_) =>
                          complete(StatusCodes.OK, outputContent)
                      }
                  }
                }
                }
              }
            }
          )
        }
      }
    }
  }
}
