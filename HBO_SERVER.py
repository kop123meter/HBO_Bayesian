import socket
import threading
import time
from datetime import datetime
import os
import json
import numpy as np
from GPyOpt.methods import BayesianOptimization
import colorama
from colorama import Fore, Style


class TwoClientsServer:

    MAX_ITER = 15
    EXPLORATION_n = 5
    NUM_TASKS = 5
    PORT = 1909

    def __init__(self):
        self.iterations = self.EXPLORATION_n + self.MAX_ITER + 1
        current_time = datetime.now()
        self.output_dir = os.path.join("./experiments", current_time.strftime("%Y-%m-%d %H-%M-%S"))
        os.makedirs(self.output_dir, exist_ok=True)
        self.stop_event = threading.Event()
        self.server_socket = None
        self.client_threads = []
        self.num_tasks_lock = threading.Lock()  # 使用锁来同步任务数量

    def run(self):
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(('0.0.0.0', self.PORT))
            self.server_socket.listen(5)

            print("SERVER: Waiting for the Python client...")

            python_client = PythonClient(num_tasks=self.NUM_TASKS,
                                         max_iter=self.MAX_ITER,
                                         host='192.168.10.164',
                                         port=self.PORT,
                                         output_dir=self.output_dir,
                                         stop_event=self.stop_event)
            python_client.start()
            self.client_threads.append(python_client)

            client_socket2, addr2 = self.server_socket.accept()
            print(f"SERVER: Python client connected: {addr2[0]} \n        now Waiting for Android client...")

            client_socket1, addr1 = self.server_socket.accept()
            print(f"SERVER: Android client connected: {addr1[0]}")

            python_handler = ClientHandler(client_socket2, client_socket1, is_android=False, output_dir=self.output_dir,
                                           stop_event=self.stop_event, num_tasks_lock=self.num_tasks_lock)
            python_handler.start()
            self.client_threads.append(python_handler)

            android_handler = ClientHandler(client_socket1, client_socket2, is_android=True, output_dir=self.output_dir,
                                            stop_event=self.stop_event, num_tasks_lock=self.num_tasks_lock)
            android_handler.start()
            self.client_threads.append(android_handler)

            while not android_handler.is_done:
                time.sleep(1)

        except Exception as e:
            print(f"SERVER: Exception: {e}")
        finally:
            self.stop()

    def stop(self):
        self.stop_event.set()
        if self.server_socket:
            self.server_socket.close()
        for thread in self.client_threads:
            thread.join()

        red_printer = printer("red")
        red_printer.print("SERVER: All sockets closed and threads terminated.")


class PythonClient(threading.Thread):
    def __init__(self, num_tasks, max_iter, host, port, output_dir, stop_event):
        super().__init__()
        self.runner = BayesianOptimizationRunner(num_tasks, max_iter, host, port, output_dir)
        self.stop_event = stop_event

    def set_NUM_TASKS(self, num_tasks):
        self.runner.set_num_tasks(num_tasks)  # 更新 BayesianOptimizationRunner 中的任务数

    def run(self):
        time.sleep(1)  # wait for the server to be ready to accept 
        self.runner.main()


class ClientHandler(threading.Thread):
    NUM_TASKS = 5

    def __init__(self, client_socket, other_client_socket, is_android=False, output_dir=None, stop_event=None, num_tasks_lock=None):
        super().__init__()
        self.client_socket = client_socket
        self.other_client_socket = other_client_socket
        self.is_android = is_android
        self.output_dir = output_dir
        self.stop_event = stop_event
        self.is_done = False
        self.num_tasks_lock = num_tasks_lock  # 用于同步任务数量
        self.NUM_TASKS = ClientHandler.NUM_TASKS  # 初始化任务数量

        if is_android:
            color_printer = printer('green')
        else:
            color_printer = printer('yellow')
        self.print = color_printer.print

        red_printer = printer('red')
        self.printRed = red_printer.print

    def set_NUM_TASKS(self, num_tasks):
        with self.num_tasks_lock:
            self.NUM_TASKS = num_tasks

    def get_NUM_TASKS(self):
        with self.num_tasks_lock:
            return self.NUM_TASKS

    def run(self):
        try:
            if self.is_android:
                self.android_client()
            else:
                self.python_client()
        except Exception as e:
            self.printRed(f"SERVER: Exception in {'Android' if self.is_android else 'Python'}ClientHandler: {e}")
        finally:
            self.print(f"SERVER: {'Android' if self.is_android else 'Python'} handler is Done")
            self.client_socket.close()
            self.is_done = True

    def python_client(self):
        while not self.stop_event.is_set():
            input_data = self.client_socket.recv(1024).decode().strip()
            if not input_data:
                break
            self.print(f"SERVER: Received from Python: {input_data}")

            if "tasknum/" in input_data:
                NUM_TASKS = int(float(input_data[len('tasknum/'):]))
                self.set_NUM_TASKS(NUM_TASKS)  # 更新 Python 客户端的 NUM_TASKS
                self.print(f"SERVER: Updated NUM_TASKS to {NUM_TASKS} for Python client.")
            else:
                self.other_client_socket.sendall((input_data + '\n').encode())

    def android_client(self):
        while not self.stop_event.is_set():
            input_data = self.client_socket.recv(1024).decode().strip()
            if not input_data:
                self.printRed("SERVER: android client 'not input_data'")
                break
            self.print(f"SERVER: Received from Android: {input_data}")

            if "thermal/" in input_data:
                filename = 'thermal_data.csv'
                filepath = os.path.join('./', self.output_dir, filename)
                with open(filepath, "a") as out:
                    out.write(input_data[len('thermal/'):] + '\n')

            elif "delegate/" in input_data:
                self.other_client_socket.sendall((input_data + '\n').encode())

            elif "tasknum/" in input_data:
                print("tasknum received" + ":::::" + "Current task number is: " + input_data[len('tasknum/'):])
                NUM_TASKS = int(float(input_data[len('tasknum/'):]))
                self.set_NUM_TASKS(NUM_TASKS)  # 更新 Android 客户端的 NUM_TASKS
                self.other_client_socket.sendall((f"tasknum/{NUM_TASKS}\n").encode())  # 发送给 Python 客户端
            else:
                self.other_client_socket.sendall((input_data + '\n').encode())


class BayesianOptimizationRunner:
    def __init__(self, num_tasks, max_iter, host, port, output_dir=None):
        self.NUM_TASKS = num_tasks
        self.MAX_ITER = max_iter
        self.HOST = host
        self.PORT = port
        self.counter = 0
        self.output_dir = output_dir
        self.client_socket = None

        color_printer = printer('blue')
        self.print = color_printer.print

    def set_num_tasks(self, num_tasks):
        self.NUM_TASKS = num_tasks

    def run(self):
        # 你的运行逻辑
        pass


class printer:
    def __init__(self, color='black'):
        self.color = color
        self.color_dict = {
            "red": Fore.RED,
            "green": Fore.GREEN,
            "yellow": Fore.YELLOW,
            "blue": Fore.BLUE,
            "magenta": Fore.MAGENTA,
            "cyan": Fore.CYAN,
            "white": Fore.WHITE,
            "black": Fore.LIGHTBLACK_EX
        }

    def print(self, text):
        color = self.color
        if color.lower() in self.color_dict:
            print(self.color_dict[color.lower()] + text + Style.RESET_ALL)
        else:
            print("Color not recognized. Available colors: red, green, yellow, blue, magenta, cyan, white, black")


if __name__ == "__main__":
    colorama.init(autoreset=True)
    server = TwoClientsServer()
    try:
        server.run()
    except KeyboardInterrupt:
        server.stop()
        red_printer = printer("red")
        red_printer.print("SERVER: Shutting down due to keyboard interrupt.")

