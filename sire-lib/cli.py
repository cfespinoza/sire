import json
import os
import tempfile
from datetime import datetime

import fire

from dataset import Dataset
from extrapolations import Extrapolations


def load_data(json_data_path: str = None):
    """
    Load the data from the json file
    :param json_data_path: the path to the json file
    """
    with open(json_data_path, "r") as f:
        json_data = json.load(f)
    return json_data


def main(database_file_path: str = "data/sapiq_db.json"
         , json_data_path: str = "data/tmp.json"
         , output_file_path: str = "data/output.json"):
    """
    Run the extrapolations
    :param database_file_path: the path to the database file
    :param json_data_path: the path to the json file
    """
    json_data = load_data(json_data_path)
    database = load_data(database_file_path) + json_data["data"]
    company_id = json_data["company_id"]
    latest_known_dt = [int(d) for d in json_data["latest_known_dt"].split("-")]  # format = "2001-12-01"
    extrapolate_len = json_data.get("extrapolate_len", 12)
    n_trials = json_data.get("n_trials", 10)
    yoy_step = json_data.get("yoy_step", 1)

    # Create a temporary json file from the data
    fd, path = tempfile.mkstemp()
    # Create Dataset using the temporary json file
    with open(path, "w") as f:
        json.dump(database, f)
    # dataset = Dataset(dataset_name="data/sapiq.json")
    dataset = Dataset(dataset_name=path)

    extrapolations = Extrapolations(
        dataset=dataset,
        org_id=company_id,
        latest_known_dt=datetime(latest_known_dt[0], latest_known_dt[1], latest_known_dt[2]),
        extrapolate_len=extrapolate_len,  # this configuration must be set in the json_data
        n_trials=n_trials,  # this configuration must be set in the json_data
        method="probability_matching",  # this configuration must be set in the json_data
        yoy_step=yoy_step,  # this configuration must be set in the json_data
    )
    extrapolations.run(params={"filter_type": "smooth"})
    predictions = extrapolations.get_extrapolations_metric_mean_confidence_interval()

    # Delete the temporary json file
    os.remove(path)
    with open(output_file_path, "w") as f:
        json.dump(predictions.to_json(orient="records", date_format="epoch"), f)


if __name__ == "__main__":
    fire.Fire(main)
