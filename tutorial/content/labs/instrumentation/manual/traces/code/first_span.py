# app.py
@tracer.start_as_current_span("index")
@app.route('/')
def index():
    # ...