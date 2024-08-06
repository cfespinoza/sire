object Constants {
  val htmlContent = """<!DOCTYPE html>
                      |<html lang="en">
                      |<head>
                      |  <meta charset="UTF-8">
                      |  <meta http-equiv="X-UA-Compatible" content="IE=edge">
                      |  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                      |  <title>Sire Server</title>
                      |<!--  <link rel="stylesheet" href="styles.css">-->
                      |  <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                      |  <script src="https://cdn.jsdelivr.net/npm/chartjs-plugin-annotation@1.3.0/dist/chartjs-plugin-annotation.min.js"></script>
                      |  <style>
                      |    #form-container {
                      |      margin-bottom: 20px;
                      |    }
                      |
                      |    label {
                      |      display: block;
                      |      margin: 10px 0 5px;
                      |    }
                      |
                      |    input {
                      |      width: 100%;
                      |      padding: 8px;
                      |      margin-bottom: 10px;
                      |    }
                      |
                      |    button {
                      |      padding: 10px 15px;
                      |      margin-right: 10px;
                      |    }
                      |
                      |    #results-container {
                      |      margin-top: 20px;
                      |    }
                      |
                      |    table {
                      |      width: 100%;
                      |      border-collapse: collapse;
                      |    }
                      |
                      |    th, td {
                      |      border: 1px solid black;
                      |      padding: 8px;
                      |      text-align: left;
                      |    }
                      |
                      |    th {
                      |      background-color: #f2f2f2;
                      |    }
                      |  </style>
                      |</head>
                      |<body>
                      |  <div id="form-container">
                      |    <form id="json-form">
                      |      <label for="company_id">Company ID:</label>
                      |      <input type="text" id="company_id" name="company_id" required>
                      |      <label for="latest_known_dt">Latest Known Date:</label>
                      |      <input type="date" id="latest_known_dt" name="latest_known_dt" required>
                      |      <label>Customer Focus (comma-separated):</label>
                      |      <input type="text" id="customer_focus" name="customer_focus">
                      |      <label>Sectors (comma-separated):</label>
                      |      <input type="text" id="sectors" name="sectors">
                      |      <div id="data-entries">
                      |        <!-- Aquí se insertarán los formularios de datos -->
                      |      </div>
                      |      <button type="button" id="addData" onclick="addDataForm()">Añadir Datos</button>
                      |      <button type="submit" id="getPredictions">Obtener Predicciones</button>
                      |      <button type="button" id="newExperimentBtn" disabled onclick="newExperiment()">Nuevo Experimento</button>
                      |    </form>
                      |  </div>
                      |  <div id="graph-container">
                      |    <canvas id="graph-canvas" width="400" height="200"></canvas>
                      |  </div>
                      |  <div id="results-container" style="display: none;">
                      |    <h2>Resultados:</h2>
                      |    <table id="results-table">
                      |      <tr>
                      |        <th>Customer</th>
                      |        <th>Sector</th>
                      |        <th>Prediction Date</th>
                      |        <th>Revenue Mean</th>
                      |        <th>Revenue 95 Lower Bound</th>
                      |        <th>Revenue 95 Upper Bound</th>
                      |        <th>Revenue Min</th>
                      |        <th>Revenue Max</th>
                      |      </tr>
                      |    </table>
                      |  </div>
                      |
                      |  <script>
                      |    document.addEventListener('DOMContentLoaded', function () {
                      |      // Asegúrate de que haya al menos 10 formularios de datos al cargar la página
                      |      const total = 1
                      |      for (let i = 0; i < total; i++) {
                      |        addDataForm();
                      |      }
                      |
                      |      document.getElementById('json-form').addEventListener('submit', function (event) {
                      |        event.preventDefault();
                      |        submitForm();
                      |      });
                      |    });
                      |
                      |    function addDataForm() {
                      |      const container = document.getElementById('data-entries');
                      |      const dataForm = document.createElement('div');
                      |      const formNumber = container.getElementsByClassName('data-form').length + 1;
                      |
                      |      dataForm.className = 'data-form';
                      |      dataForm.innerHTML = `
                      |        <h4>Entrada ${formNumber}:</h4>
                      |        <label>Date:</label>
                      |        <input type="date" name="date" required>
                      |        <label>Revenue:</label>
                      |        <input type="number" step="any" name="revenue" required>
                      |        <label>YOY Growth:</label>
                      |        <input type="number" step="any" name="yoy_growth" required>
                      |        <label>Next YOY Growth:</label>
                      |        <input type="number" step="any" name="next_yoy_growth" required>
                      |        <hr>
                      |      `;
                      |
                      |      container.appendChild(dataForm);
                      |    }
                      |
                      |    function submitForm() {
                      |        // Deshabilita los botones de solicitud y habilita el botón de nuevo experimento
                      |      document.getElementById("addData").disabled = true;
                      |      document.getElementById("getPredictions").disabled = true;
                      |      document.getElementById("newExperimentBtn").disabled = false;
                      |      const companyID = document.getElementById('company_id').value;
                      |      const latestKnownDt = document.getElementById('latest_known_dt').value;
                      |      // const sectors = document.getElementById('sectors').value.split(',').map(s => s.trim());
                      |      let sectors = []
                      |      if (document.getElementById('sectors').value.trim() !== '') {
                      |          sectors = document.getElementById('sectors').value.split(',').map(s => s.trim());
                      |      }
                      |      // const customerFocus = document.getElementById('customer_focus').value.split(',').map(s => s.trim());
                      |      let customerFocus = []
                      |      if (document.getElementById('customer_focus').value.trim() !== '') {
                      |        customerFocus = document.getElementById('customer_focus').value.split(',').map(s => s.trim());
                      |      }
                      |      const dataForms = document.getElementsByClassName('data-form');
                      |
                      |      const jsonData = {
                      |        company_id: companyID,
                      |        latest_known_dt: latestKnownDt,
                      |        data: []
                      |      };
                      |
                      |      Array.from(dataForms).forEach(form => {
                      |        const data = {};
                      |        Array.from(form.querySelectorAll('input')).forEach(input => {
                      |          const key = input.name;
                      |          const value = input.value;
                      |          if (key === 'date') {
                      |            const dateObject = new Date(value); // Convierte la string a un objeto Date
                      |            const milliseconds = dateObject.getTime(); // Convierte el objeto Date a milisegundos
                      |            data[key] = milliseconds;
                      |          } else {
                      |            data[key] = parseFloat(value);
                      |          }
                      |          // data[key] = value;
                      |          data["id"] = companyID;
                      |          data["sectors"] = sectors;
                      |          data["customer_focus"] = customerFocus;
                      |        });
                      |        jsonData.data.push(data);
                      |      });
                      |
                      |      fetch('/predict', {
                      |      // fetch('http://localhost:5050/predict', {
                      |        method: 'POST',
                      |        headers: {
                      |          'Content-Type': 'application/json'
                      |        },
                      |        body: JSON.stringify(jsonData)
                      |      })
                      |      .then(response => response.json())
                      |      .then(data => {
                      |        let parsedData = Object.keys(data).map(key => {
                      |          let customer = key;
                      |          let sector = key;
                      |          if (key.includes("_")) {
                      |            customer = key.split("_")[0];
                      |            sector = key.split("_")[1];
                      |          }
                      |          const dataset = data[key].map(item => {
                      |            item.customer = customer;
                      |            item.sector = sector;
                      |            if (customer === sector){
                      |              item.revenue_95_lower_bound = "";
                      |              item.revenue_95_upper_bound = "";
                      |              item.revenue_max = "";
                      |              item.revenue_min = "";
                      |            }
                      |            return item;
                      |          })
                      |          return dataset;
                      |        });
                      |        const flatten = parsedData.flat();
                      |        console.log(flatten);
                      |        displayResults(flatten);
                      |        drawResults(data);
                      |      })
                      |      .catch(error => console.error('Error:', error));
                      |    }
                      |    function newExperiment() {
                      |      // Recarga la página para volver al estado inicial
                      |      location.reload();
                      |    }
                      |    function displayResults(data) {
                      |      const resultsContainer = document.getElementById('results-container');
                      |      const resultsTable = document.getElementById('results-table');
                      |
                      |      // Limpiar resultados previos
                      |      while (resultsTable.rows.length > 1) {
                      |        resultsTable.deleteRow(1);
                      |      }
                      |
                      |      // Mostrar los resultados
                      |      data.forEach(item => {
                      |        const row = resultsTable.insertRow();
                      |        const sorted_keys = ["customer", "sector", "prediction_date", "revenue_mean", "revenue_95_lower_bound",
                      |          "revenue_95_upper_bound", "revenue_min", "revenue_max"]
                      |        sorted_keys.forEach(key => {
                      |            const cell = row.insertCell();
                      |            cell.textContent = item[key];
                      |            });
                      |      });
                      |
                      |      resultsContainer.style.display = 'block';
                      |    }
                      |
                      |    function drawResults(data) {
                      |        // Función para convertir los datos
                      |        const convertData = (seriesData, label, color) => {
                      |            return {
                      |                label: label,
                      |                data: seriesData.map(item => ({
                      |                    x: new Date(parseInt(item.prediction_date)).toLocaleDateString(),
                      |                    y: item.revenue_mean
                      |                })),
                      |                borderColor: color,
                      |                backgroundColor: color,
                      |                fill: false,
                      |                tension: 0.1
                      |            };
                      |        };
                      |
                      |        const labels = ['real', 'mean'];
                      |        const colors = { 'real': 'blue', 'mean': 'red' };
                      |        const datasets = [];
                      |
                      |        Object.keys(data).forEach(key => {
                      |            const color = labels.includes(key) ? colors[key] : 'gray';
                      |            datasets.push(convertData(data[key], key, color));
                      |        });
                      |
                      |        const graph_labels = Object.keys(data).map(key => new Date(parseInt(data[key].prediction_date)).toLocaleDateString());
                      |        const fechas = [...new Set(graph_labels)];
                      |
                      |
                      |        const ctx = document.getElementById('graph-canvas').getContext('2d');
                      |        const grafico = new Chart(ctx, {
                      |            type: 'line',
                      |            data: {
                      |                labels: fechas,
                      |                datasets: datasets
                      |            },
                      |            options: {
                      |                scales: {
                      |                    x: {
                      |                        beginAtZero: true,
                      |                        title: {
                      |                            display: true,
                      |                            text: 'Fecha de Predicción'
                      |                        }
                      |                    },
                      |                    y: {
                      |                        beginAtZero: true,
                      |                        title: {
                      |                            display: true,
                      |                            text: 'Ingresos medios'
                      |                        }
                      |                    }
                      |                }
                      |            }
                      |        });
                      |    }
                      |
                      |  </script>
                      |</body>
                      |</html>
                      |""".stripMargin
}
