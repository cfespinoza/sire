import json
import os
import unittest

import pytest
import requests_mock

from sire_client.client import SireClient
from sire_client.constants import PREDICT_ENDPOINT, HEALTH_ENDPOINT, REQUEST_SAMPLE_DATA


class TestSireClient(unittest.TestCase):

    def setUp(self):
        self.url = os.environ.get("SIRE_SERVER", "http://example.com")
        self.health_endpoint = HEALTH_ENDPOINT
        self.predict_endpoint = PREDICT_ENDPOINT
        self.successful_response = {"predictions": []}
        self.failed_response = {"error": "Bad Request"}

    @requests_mock.Mocker()
    def test_sire_client_successful_health_check(self, m):
        m.get(self.url + self.health_endpoint, status_code=200, text="Healthy")

        # Asumimos que si no se lanza una excepción, la inicialización fue exitosa
        client = SireClient(url=self.url, health_endpoint=self.health_endpoint)

    @requests_mock.Mocker()
    def test_sire_client_failed_health_check(self, m):
        m.get(self.url + self.health_endpoint, status_code=500, text="Not Healthy")

        with self.assertRaises(Exception) as context:
            client = SireClient(url=self.url, health_endpoint=self.health_endpoint)

        self.assertTrue("Sire server is not healthy." in str(context.exception))

    @requests_mock.Mocker()
    def test_sire_client_successful_predict(self, m):
        m.post(self.url + self.predict_endpoint, status_code=200, text=json.dumps(self.successful_response))
        m.get(self.url + self.health_endpoint, status_code=200, text="Healthy")
        client = SireClient(url=self.url, predict_endpoint=self.predict_endpoint)
        client.predict(data=[])

    @requests_mock.Mocker()
    def test_sire_client_failed_predict(self, m):
        m.post(self.url + self.predict_endpoint, status_code=400, text=json.dumps(self.failed_response))
        m.get(self.url + self.health_endpoint, status_code=200, text="Healthy")
        client = SireClient(url=self.url, predict_endpoint=self.predict_endpoint)
        client.predict(data={})

    @pytest.mark.integration
    def test_sire_server_client_successful_predict(self):
        client = SireClient(url=self.url, predict_endpoint=self.predict_endpoint)
        response = client.predict(data=REQUEST_SAMPLE_DATA)
        print(response.text)
        self.assertEqual(response.status_code, 200)
