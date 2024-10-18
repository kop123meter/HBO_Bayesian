import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import numpy as np
from PIL import Image

# Load the original image
image_path = '/mnt/data/image.png'
original_image = Image.open(image_path)

# Create a new figure
fig, ax = plt.subplots(figsize=(10, 8))

# Display the original image
ax.imshow(original_image)
ax.axis('off')  # Hide axes

# Data for the new bar chart
# Bar heights and labels
categories = ['SC1-CF1', 'SC2-CF1', 'SC1-CF2', 'SC2-CF2']
server_tasks = [3, 3, 1, 0]  # Total tasks completed by server
gpu_tasks = [2, 0, 0, 0]      # GPU tasks
cpu_tasks = [1, 0, 0, 0]       # CPU tasks

# Bar widths
bar_width = 0.4
x_indices = np.arange(len(categories))

# Create the bars
bars1 = ax.bar(x_indices - bar_width/2, server_tasks, width=bar_width, color='orange', label='Server Tasks')
bars2 = ax.bar(x_indices + bar_width/2, gpu_tasks, width=bar_width, color='gray', label='GPU Tasks')
bars3 = ax.bar(x_indices + bar_width/2, cpu_tasks, width=bar_width, color='lightgray', label='CPU Tasks')

# Adding the legend
ax.legend(loc='upper right')

# Title and labels
plt.title('Task Completion by Server, GPU, and CPU', fontsize=16)
plt.xlabel('Categories', fontsize=14)
plt.ylabel('# of Tasks', fontsize=14)

# Set x-ticks
ax.set_xticks(x_indices)
ax.set_xticklabels(categories)

# Show the plot
plt.tight_layout()
plt.show()
