document.addEventListener('DOMContentLoaded', function () {
  // Asegúrate de que haya al menos 10 formularios de datos al cargar la página
  const total = 1
  for (let i = 0; i < total; i++) {
    addDataForm();
  }

  document.getElementById('json-form').addEventListener('submit', function (event) {
    event.preventDefault();
    submitForm();
  });
});

function addDataForm() {
  const container = document.getElementById('data-entries');
  const dataForm = document.createElement('div');
  const formNumber = container.getElementsByClassName('data-form').length + 1;

  dataForm.className = 'data-form';
  dataForm.innerHTML = `
    <h4>Entrada ${formNumber}:</h4>
    <label>Date (in milliseconds):</label>
    <input type="number" name="date" required>
    <label>Revenue:</label>
    <input type="number" step="any" name="revenue" required>
    <label>YOY Growth:</label>
    <input type="number" step="any" name="yoy_growth" required>
    <label>Next YOY Growth:</label>
    <input type="number" step="any" name="next_yoy_growth" required>
    <hr>
  `;

  container.appendChild(dataForm);
}

function submitForm() {
    // Deshabilita los botones de solicitud y habilita el botón de nuevo experimento
  document.getElementById("addData").disabled = true;
  document.getElementById("getPredictions").disabled = true;
  document.getElementById("newExperimentBtn").disabled = false;
  const companyID = document.getElementById('company_id').value;
  const latestKnownDt = document.getElementById('latest_known_dt').value;
  const sectors = document.getElementById('sectors').value.split(',').map(s => s.trim());
  const customerFocus = document.getElementById('customer_focus').value.split(',').map(s => s.trim());
  const dataForms = document.getElementsByClassName('data-form');

  const jsonData = {
    company_id: companyID,
    latest_known_dt: latestKnownDt,
    data: []
  };

  Array.from(dataForms).forEach(form => {
    const data = {};
    Array.from(form.querySelectorAll('input')).forEach(input => {
      const key = input.name;
      const value = input.value;
      if (key === 'date') {
        data[key] = parseInt(value);
      } else {
        data[key] = parseFloat(value);
      }
      // data[key] = value;
      data["id"] = companyID;
      data["sectors"] = sectors;
      data["customer_focus"] = customerFocus;
    });
    jsonData.data.push(data);
  });

  // fetch('http://sire-server-5c6aed86c9ce.herokuapp.com/predict', {
  fetch('http://localhost:5050/predict', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(jsonData)
  })
  .then(response => response.json())
  .then(data => {
    const parsedData = JSON.parse(data);
    displayResults(parsedData);
    drawResults(parsedData);
  })
  .catch(error => console.error('Error:', error));
}
function newExperiment() {
  // Recarga la página para volver al estado inicial
  location.reload();
}
function displayResults(data) {
  const resultsContainer = document.getElementById('results-container');
  const resultsTable = document.getElementById('results-table');

  // Limpiar resultados previos
  while (resultsTable.rows.length > 1) {
    resultsTable.deleteRow(1);
  }

  // Mostrar los resultados
  data.forEach(item => {
    const row = resultsTable.insertRow();
    Object.values(item).forEach(value => {
      const cell = row.insertCell();
      cell.textContent = value;
    });
  });

  resultsContainer.style.display = 'block';
}

function drawResults(data) {
    const fechas = data.map(d => new Date(d.prediction_date).toLocaleDateString());
    const revenueMean = data.map(d => d.revenue_mean);
    const revenue95Lower = data.map(d => d.revenue_95_lower_bound);
    const revenue95Upper = data.map(d => d.revenue_95_upper_bound);
    const revenueMin = data.map(d => d.revenue_min);
    const revenueMax = data.map(d => d.revenue_max);

    const ctx = document.getElementById('graph-canvas').getContext('2d');
    const grafico = new Chart(ctx, {
        type: 'line',
        data: {
            labels: fechas,
            datasets: [{
                label: 'Media de Ingresos',
                data: revenueMean,
                borderColor: 'red',
                backgroundColor: 'rgba(255, 0, 0, 0.1)',
            }, {
                label: 'Límite Inferior del 95%',
                data: revenue95Lower,
                borderColor: 'rgba(0, 0, 0, 0)',
                fill: '+1', // Rellena hacia el próximo dataset
            }, {
                label: 'Límite Superior del 95%',
                data: revenue95Upper,
                borderColor: 'rgba(0, 0, 0, 0)',
                fill: false,
                backgroundColor: 'rgba(255, 0, 0, 0.1)', // Color de relleno
            }, {
                label: 'Ingresos Mínimos',
                data: revenueMin,
                borderColor: 'rgba(0, 0, 0, 0)',
                fill: '+1',
            }, {
                label: 'Ingresos Máximos',
                data: revenueMax,
                borderColor: 'rgba(0, 0, 0, 0)',
                fill: false,
                backgroundColor: 'rgba(0, 255, 0, 0.1)', // Color de relleno

            }],
        },
        options: {
            scales: {
                x: {
                    beginAtZero: true,
                    title: {
                        display: true,
                        text: 'Fecha de Predicción'
                    }
                },
                y: {
                    beginAtZero: true,
                    title: {
                        display: true,
                        text: 'Ingresos'
                    }
                }
            }
        }
    });
}
