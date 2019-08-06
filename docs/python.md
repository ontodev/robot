# Python

The `python` command enables access of ROBOT operations and objects in Python using [Py4J](https://www.py4j.org/). This command does not accept any input ontologies and does not produce any outputs. You *cannot* chain this command with other ROBOT commands.

By default, the `python` command starts a Py4J gateway server on port `25333` (the Py4J default). To change this, use the `--port` option:
```
robot python --port 8000
```

Once the server has started, you can dynamically access ROBOT through the gateway in a Python script. You can access any ROBOT objects through the gateway, for example, the `IOHelper` to load an ontology:
```
from py4j.java_gateway import JavaGateway

gateway = JavaGateway()

io_helper = gateway.jvm.org.obolibrary.robot.IOHelper()

ont = io_helper.loadOntology('my-ontology.owl')
```

Objects must start with `gateway.jvm` followed by the package name (e.g., `org.obolibrary.robot`).

Once finished, you can stop the gateway server from the command line with Ctrl+C.