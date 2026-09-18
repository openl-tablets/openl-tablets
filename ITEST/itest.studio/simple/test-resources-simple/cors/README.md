# CORS with `cors.allowed.origins = *`

The asterisk lists every origin: any origin's pre-flight is answered by the CORS filter itself, and any origin's
read carries `Access-Control-Allow-Origin`. Header values are compared exactly, so a second registration of the
filter — which would repeat `Vary: Origin` on every response but the pre-flight answer — fails the other probes.
