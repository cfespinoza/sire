import logging

import requests

from sire_client.constants import HEALTH_ENDPOINT, PREDICT_ENDPOINT


class SireClient:
    """
    Rest client for Sire server.
    """
    url: str
    health_endpoint: str
    predict_endpoint: str

    def __init__(self,
                 url: str,
                 health_endpoint: str = HEALTH_ENDPOINT,
                 predict_endpoint: str = PREDICT_ENDPOINT):
        self.logger = logging.getLogger(__name__)
        self.url = url
        self.health_endpoint = health_endpoint
        self.predict_endpoint = predict_endpoint
        self._check_health()

    def _check_health(self):
        """
        Check if the sire server is healthy.
        """
        request_url = self.url + self.health_endpoint
        response = requests.get(request_url)
        self.logger.info(f"{request_url} returned status code: {response.status_code}")
        self.logger.info(f"{request_url} returned message: {response.text}")
        if response.status_code != 200:
            self.logger.error("Sire server is not healthy.")
            raise Exception("Sire server is not healthy.")

    def predict(self, data: list):
        """
        Request a prediction to the sire server.
        """
        request_url = self.url + self.predict_endpoint
        request_body = {
            "data": data
        }
        response = requests.post(request_url, json=request_body)
        self.logger.info(f"{request_url} returned status code: {response.status_code}")
        return response

    def predict_from_json(self, data: list):
        """

        :param data: data sent to server to make predictions. Example:
        [{ "date": 1080777600000, "revenue": 1031.612874495, "yoy_growth": 1.3232494523, "next_yoy_growth": 1.221329417},
        { "date": 1083369600000, "revenue": 1259.9391506252, "yoy_growth": 1.221329417, "next_yoy_growth": 1.8539041857},
        { "date": 1086048000000, "revenue": 2335.8064650143, "yoy_growth": 1.8539041857, "next_yoy_growth": 1.4894796637},
        { "date": 1088640000000, "revenue": 3479.1362280076, "yoy_growth": 1.4894796637, "next_yoy_growth": 1.043030377},
        { "date": 1091318400000, "revenue": 3628.8447715131, "yoy_growth": 1.043030377, "next_yoy_growth": 1.4719744715},
        { "date": 1093996800000, "revenue": 5341.5668646305, "yoy_growth": 1.4719744715, "next_yoy_growth": 0.8346232223},
        { "date": 1096588800000, "revenue": 4458.1957488909, "yoy_growth": 0.8346232223, "next_yoy_growth": 0.6058455305},
        { "date": 1099267200000, "revenue": 2700.9779683877, "yoy_growth": 0.6058455305, "next_yoy_growth": 0.4461212517},
        { "date": 1101859200000, "revenue": 1204.963672196, "yoy_growth": 0.4461212517, "next_yoy_growth": 0.5163532691}]
        :return:
        """
        pass
