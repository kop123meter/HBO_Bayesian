import socket
import matplotlib.pyplot as plt
import matplotlib.animation as animation
from threading import Thread

# Save rewards and triangle counts
rewards = []
tris_counts = []
AIlats = []

# Server to receive data from the client
def socket_server():
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind(('0.0.0.0', 3434))
    server_socket.listen(5)
    print("Server listening on port 3434")

    while True:
        client_socket, addr = server_socket.accept()
        print(f"Connection from {addr}")

        while True:
            data = client_socket.recv(1024).decode()
            if not data:
                break
            try:
                reward, current_tris, latency = map(float, data.split(","))
                rewards.append(reward)
                tris_counts.append(current_tris)
                AIlats.append(latency)
                print(f"Received: reward={reward}, current_tris={current_tris}, latency={latency}")
            except ValueError:
                print("Invalid data received")
        
        client_socket.close()
        print(f"Connection closed with {addr}")

# Update the plot
def update_plot(i):
    ax1.clear()
    ax2.clear()
    ax3.clear()

    ax1.set_title('Reward')
    ax1.plot(rewards, label='Reward',color='r')
    ax1.legend(loc='upper left')
    ax1.set_xlabel('Episodes')
    ax1.set_ylabel('Reward')

    ax2.set_title('Triangle Count')
    ax2.plot(tris_counts, label='Triangle Count', color='b')
    ax2.legend(loc='upper left')
    ax2.set_xlabel('Episodes')
    ax2.set_ylabel('Triangle Count')

    ax3.set_title('AI Latency')
    ax3.plot(AIlats, label='AI Latency', color='g')
    ax3.legend(loc='upper left')
    ax3.set_xlabel('Episodes')
    ax3.set_ylabel('AI Latency')


    plt.tight_layout()

if __name__ == '__main__':
    # Start the server in a separate thread
    t1 = Thread(target=socket_server)
    t1.start()

    # Set up the figure and axis
    fig, (ax1, ax2, ax3) = plt.subplots(3, 1, figsize=(10, 10))

    # Plot the data
    ani = animation.FuncAnimation(fig, update_plot, interval=1000)
    plt.show()

    # Wait for the server thread to finish
    t1.join()
