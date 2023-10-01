import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat
import akka.Done

import java.io.{File, PrintWriter}
import scala.io.StdIn
import scala.sys.process._
import scala.util.{Failure, Success, Try}

object Server extends App {

  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "sire-server")
  implicit val executionContext = system.executionContext
  implicit val materializer: Materializer = Materializer(system)
  val route: Route = concat(
    get {
      pathSingleSlash {
        complete("Welcome to SIRE!")
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
              val executable = "/home/charles/Workspace/python_projects/olbap-cafe/sire/sire-lib/.venv/bin/python"
              val scriptPath = "/home/charles/Workspace/python_projects/olbap-cafe/sire/sire-lib/cli.py"
              val databaseFilePath = "/home/charles/Workspace/python_projects/olbap-cafe/sire/sire-lib/data/sapiq_db.json"
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
  val bindingFuture = Http().newServerAt("localhost", 8080).bind(route)

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

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
