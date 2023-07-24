package com.arcore.AI_ResourceControl;// MyPythonRunner.java
// MyPythonRunner.java
import org.python.core.Py;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

public class MyPythonRunner {
    public void runPythonCode() {
        // Set up the Python interpreter
        PySystemState.initialize();
        PythonInterpreter interpreter = new PythonInterpreter(null, new PySystemState());

        try {
            // Execute Python code
            interpreter.exec("print('Hello from Python!')");
            interpreter.exec("result = 2 + 3");

            // Access the Python variables in Java
            int result = interpreter.get("result", Integer.class);
            System.out.println("Result from Python: " + result);
        } finally {
            // Clean up resources
            interpreter.cleanup();
            interpreter.close();
        }
    }
}
