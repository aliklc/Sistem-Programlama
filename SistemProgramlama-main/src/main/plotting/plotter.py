import socket
import struct
from capacity_pb2 import Capacity
import matplotlib.pyplot as plt
from collections import deque
import threading
import traceback
import time

PLOTTING_SERVER_PORT = 7000
MAX_DATA_POINTS = 300

server_capacity_data = {
    'server1': deque(maxlen=MAX_DATA_POINTS),
    'server2': deque(maxlen=MAX_DATA_POINTS),
    'server3': deque(maxlen=MAX_DATA_POINTS)
}

data_lock = threading.Lock()
start_time = time.time()

def plot_capacity_data():
    plt.ion()
    fig, ax = plt.subplots(figsize=(12, 6))

    server_styles = {
        'server1': {'color': 'purple', 'linestyle': '-', 'label': 'Server 1'},  # Düz çizgi
        'server2': {'color': 'grey', 'linestyle': '--', 'label': 'Server 2'},   # Kesik çizgi
        'server3': {'color': 'blue', 'linestyle': ':', 'label': 'Server 3'}     # Noktalarla çizgi
    }

    lines = {}

    ax.set_xlabel("Time (seconds)")
    ax.set_ylabel("Capacity")

    plt.tight_layout()

    while True:
        with data_lock:
            active_servers = [
                server for server, data in server_capacity_data.items() if data
            ]

            for server_name in active_servers:
                if server_name not in lines:
                    style = server_styles[server_name]
                    lines[server_name] = ax.plot([], [],
                                                 color=style['color'],
                                                 linestyle=style['linestyle'],
                                                 label=style['label']
                                                 )[0]

                data = server_capacity_data[server_name]
                times = [point[1] for point in data]
                capacities = [point[0] for point in data]

                lines[server_name].set_data(times, capacities)

            if active_servers:
                max_time = max(
                    max(point[1] for point in server_capacity_data[server])
                    for server in active_servers
                )
                ax.set_xlim(0, max_time + 5)

                ax.legend()

        ax.relim()
        ax.autoscale_view(scaley=True, scalex=False)
        plt.pause(5)  

def handle_client_connection(connection):
    while True:
        try:
            length_data = connection.recv(4)
            if not length_data:
                break
            message_length = struct.unpack("!I", length_data)[0]

            received_data = connection.recv(message_length)
            capacity = Capacity()

            try:
                capacity.ParseFromString(received_data)
            except Exception as e:
                print(f"Failed to parse the capacity data: {e}")
                print(traceback.format_exc())
                continue

            print(f"Received capacity - Server: Server{capacity.server_id}, Status: {capacity.serverX_status}, Timestamp: {capacity.timestamp}")

            with data_lock:
                current_time = time.time() - start_time
                server_name = f"server{capacity.server_id}"
                if server_name in server_capacity_data:
                    server_capacity_data[server_name].append((capacity.serverX_status, current_time))
                else:
                    print(f"Unknown server ID: {capacity.server_id}")

        except Exception as e:
            print(f"Error while processing data from the server: {e}")
            print(traceback.format_exc())
            break


def start_plotting_server():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as server:
        server.bind(('localhost', PLOTTING_SERVER_PORT))
        server.listen(1)
        print(f"Plotting server is listening on port {PLOTTING_SERVER_PORT}...")

        while True:
            connection, address = server.accept()
            print(f"Client connected: {address}")
            handle_client_connection(connection)

if __name__ == "__main__":
    server_thread = threading.Thread(target=start_plotting_server, daemon=True)
    server_thread.start()

    plot_capacity_data()