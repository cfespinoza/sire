import json
import os
import tempfile
from datetime import datetime

import fire
import warnings

import pandas as pd

from dataset import Dataset
from extrapolations import Extrapolations

warnings.filterwarnings("ignore")

sectors = ["3fbe5ed156f149939d9396e729af009070ccc54984b5bd8e088d86a72f8ac879",
           "4bf3fcb1090c5362f115577582e490079c659107fa05bad1a54d239b95a1abf7",
           "776de885df4110bf359fdbf78f2b39059d5da13a1b79f49f2e1963e3cec23580",
           "6d4a245c55c115dfc0f1152d9383924c3ef68c1f03951332542d867e19de865f"]
customers = ["3345821e031f176625606aa47a2e8bf7492f22e19315924a141371a9349915e8",
             "5527f7356a87cec5d061767332baf6dd52a8d98ad469704be27e6eb403ab92e7"]


def load_data(json_data_path: str = None):
    """
    Load the data from the json file
    :param json_data_path: the path to the json file
    """
    with open(json_data_path, "r") as f:
        json_data = json.load(f)
    return json_data


def main_grouped(database_file_path: str = "data/sapiq_db.json"
                 , json_data_path: str = "data/tmp.json"
                 , output_file_path: str = "data/output.json"):
    with open(json_data_path, "r") as f:
        current_company_info = json.load(f)

    base_data = dict(current_company_info["data"][0])
    current_data = dict(base_data)
    predictions = {}
    total_data = []
    for sector in sectors:
        for customer in customers:
            timestamp = datetime.now().timestamp()
            formatted_timestamp = str(int(timestamp))
            current_data["sectors"] = [sector]
            current_data["customer_focus"] = [customer]
            es_correcto = all([x in base_data["sectors"] and y in base_data["customer_focus"]
                               for x in current_data["sectors"] for y in current_data["customer_focus"]])
            # Crear y escribir en el fichero JSON
            # json_file_name = f'./tmp/{formatted_timestamp}.json'
            fd, json_file_name = tempfile.mkstemp()
            with open(json_file_name, 'w') as f:
                json.dump(current_company_info, f)
            main(database_file_path=database_file_path,
                 json_data_path=json_file_name,
                 output_file_path=json_file_name)
            with open(json_file_name, "r") as f:
                prediction_data = json.loads(json.load(f))
            os.remove(json_file_name)
            total_data.extend(prediction_data)
            if es_correcto:
                predictions["real"] = prediction_data
            else:
                predictions[f"{customer}_{sector}"] = prediction_data

    total_data_df = pd.DataFrame(total_data)
    media = [{"prediction_date": k, "revenue_mean": v} for k, v in
             json.loads(total_data_df.groupby('prediction_date')["revenue_mean"].mean().to_json()).items()]
    predictions["mean"] = media

    with open(output_file_path, "w") as f:
        json.dump(predictions, f)


def main(database_file_path: str = "data/sapiq_db.json"
         , json_data_path: str = "data/tmp.json"
         , output_file_path: str = "data/output.json"):
    """
    Run the extrapolations
    :param output_file_path:
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
    fire.Fire(main_grouped)
