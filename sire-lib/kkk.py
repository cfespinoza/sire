if __name__ == "__main__":
    base_data = {
        "id": "23d62860a03b91c69058cccc07fc545910a1c115d9fb22ac09b79365b4c0369f",
        "date": 986083200000,
        "customer_focus": [
        ],
        "sectors": [
        ],
        "revenue": 205.4970259821,
        "yoy_growth": 1.0004409029,
        "next_yoy_growth": 0.8096898135
    }
    base_company_info = {
        "company_id": "23d62860a03b91c69058cccc07fc545910a1c115d9fb22ac09b79365b4c0369f",
        "latest_known_dt": "2011-11-01",
        "data": []
    }
    sectors = ["3fbe5ed156f149939d9396e729af009070ccc54984b5bd8e088d86a72f8ac879",
               "4bf3fcb1090c5362f115577582e490079c659107fa05bad1a54d239b95a1abf7",
               "776de885df4110bf359fdbf78f2b39059d5da13a1b79f49f2e1963e3cec23580",
               "6d4a245c55c115dfc0f1152d9383924c3ef68c1f03951332542d867e19de865f"]
    customers = ["3345821e031f176625606aa47a2e8bf7492f22e19315924a141371a9349915e8",
                 "5527f7356a87cec5d061767332baf6dd52a8d98ad469704be27e6eb403ab92e7"]

    import json
    import time
    from datetime import datetime
    import matplotlib.pyplot as plt
    from cli import main as predictor
    import pandas as pd

    # Obtener el timestamp actual

    sector_fijo = dict(base_data)
    sector_fijo["sectors"] = sectors[0:1]

    plt.figure(figsize=(10, 5))
    plt.clf()
    for customer in customers:
        timestamp = datetime.now().timestamp()
        formatted_timestamp = str(int(timestamp))
        current_data = dict(sector_fijo)
        current_data["customer_focus"] = [customer]
        current_company_info = dict(base_company_info)
        current_company_info["data"].append(current_data)
        # Crear y escribir en el fichero JSON
        json_file_name = f'./tmp/{formatted_timestamp}.json'
        with open(json_file_name, 'w') as f:
            json.dump(current_company_info, f)
        predictor(database_file_path="data/sapiq.json",
                  json_data_path=json_file_name,
                  output_file_path=json_file_name)
        with open(json_file_name, "r") as fp:
            prediction_data = json.loads(fp.read())

        print(prediction_data[0])
        predictions = pd.DataFrame(prediction_data, index=["prediction_date"],
                                   columns=["prediction_date", "revenue_mean", "revenue_95_lower_bound",
                                            "revenue_95_upper_bound", "revenue_min", "revenue_max"])
        predictions.set_index("prediction_date")
        plt.plot(predictions["revenue_mean"], label=customer)
        plt.fill_between(predictions["revenue_mean"], label=customer)

    plt.xlabel('prediction_date')
    plt.ylabel('revenue_mean')
    plt.title('Predicciones')
    plt.legend()

    # Mostrar el plot
    plt.show()

