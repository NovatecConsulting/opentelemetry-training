import time

import requests
from client import ChaosClient, FakerClient
from flask import Flask, Response, make_response, request
from metric_utils import (
    create_meter,
    create_request_instruments,
    create_resource_instruments,
)

# global variables
app = Flask(__name__)


@app.before_request
def before_request():
    request_instruments["traffic_volume"].add(
        1, attributes={"http.route": request.path}
    )
    request.environ["request_start"] = time.time_ns()


@app.after_request
def after_request(response: Response) -> Response:
    if response.status_code >= 400:
        request_instruments["error_rate"].add(1, {
                "http.route": request.path,
                "http.response.status_code": response.status_code
            }
        )
    request_end = time.time_ns()
    duration = (request_end - request.environ["request_start"]) / 1_000_000_000 # convert ns to s
    request_instruments["request_latency"].record(
        duration,
        attributes={
            "http.request.method": request.method,
            "http.route": request.path,
            "http.response.status_code": response.status_code,
        }
    )
    return response


@app.route("/users", methods=["GET"])
def get_user():
    user, status = db.get_user(123)
    data = {}
    if user is not None:
        data = {"id": user.id, "name": user.name, "address": user.address}
    response = make_response(data, status)
    return response


def do_stuff():
    time.sleep(0.1)
    url = "http://localhost:6000/"
    response = requests.get(url)
    return response


@app.route("/", methods=["GET", "POST"])
def index():
    do_stuff()
    current_time = time.strftime("%a, %d %b %Y %H:%M:%S", time.gmtime())
    # workload_instruments['traffic_volume'].add(1, {"result": choice(['win', 'lose', 'draw']) })
    return f"Hello, World! It's currently {current_time}"


if __name__ == "__main__":
    # setup metrics
    meter = create_meter("app.py", "0.1")
    request_instruments = create_request_instruments(meter)
    rc_instruments = create_resource_instruments(meter)

    db = ChaosClient(client=FakerClient())
    app.run(host="0.0.0.0", debug=False)
