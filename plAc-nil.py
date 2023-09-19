#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Sun Sep 17 13:40:34 2023

@author: niloofar
"""


'''
def plt_acquisition(self, filename=None, label_x=None, label_y=None):
        """
        Plots the model and the acquisition function.
            if self.input_dim = 1: Plots data, mean and variance in one plot and the acquisition function in another plot
            if self.input_dim = 2: as before but it separates the mean and variance of the model in two different plots
        :param filename: name of the file where the plot is saved
        :param label_x: Graph's x-axis label, if None it is renamed to 'x' (1D) or 'X1' (2D)
        :param label_y: Graph's y-axis label, if None it is renamed to 'f(x)' (1D) or 'X2' (2D)
        """
        if self.model.model is None:
            from copy import deepcopy
            model_to_plot = deepcopy(self.model)
            if self.normalize_Y:
                Y = normalize(self.Y, self.normalization_type)
            else:
                Y = self.Y
            model_to_plot.updateModel(self.X, Y, self.X, Y)
        else:
            model_to_plot = self.model
        
        return plt_acquisition2(self.acquisition.space.get_bounds(),
                                model_to_plot.model.X.shape[1],
                                model_to_plot.model,
                                model_to_plot.model.X,
                                model_to_plot.model.Y,
                                self.acquisition.acquisition_function,
                                self.suggest_next_locations(),
                                filename,
                                label_x,
                                label_y)

def plt_acquisition2(bounds, input_dim, model, Xdata, Ydata, acquisition_function, suggested_sample,
                     filename=None, label_x=None, label_y=None, color_by_step=True):
    
    
        label_x, label_y, label_z, label_w =  ('C', 'G', 'N', 'T')

        X1 = np.linspace(bounds[0][0], bounds[0][1], 50)  # Modify grid size as needed
        X2 = np.linspace(bounds[1][0], bounds[1][1], 50)  # Modify grid size as needed
        X3 = np.linspace(bounds[2][0], bounds[2][1], 50)  # Modify grid size as needed
        X4 = np.linspace(bounds[3][0], bounds[3][1], 50)  # Modify grid size as needed

        # Create a 4D grid
        x1, x2, x3, x4 = np.meshgrid(X1, X2, X3, X4)
        X = np.column_stack((x1.ravel(), x2.ravel(), x3.ravel(), x4.ravel()))

        acqu = acquisition_function(X)
        acqu_normalized = (-acqu - min(-acqu)) / (max(-acqu - min(-acqu)))
        acqu_normalized = acqu_normalized.reshape(x1.shape)
        m, v = model.predict(X)

        fig = plt.figure(figsize=(15, 5))
        
        # Create 4 subplots for each pair of dimensions
        for i in range(4):
            ax = fig.add_subplot(1, 4, i + 1, projection='3d')
            ax.contourf(x1[:, :, 0, 0], x2[:, :, 0, 0], acqu_normalized[:, :, 0, 0], 100)
            ax.scatter(suggested_sample[:, 0], suggested_sample[:, 1], suggested_sample[:, 2], c='m', marker='o', s=50, label='Suggested Samples')
            ax.set_xlabel(label_x)
            ax.set_ylabel(label_y)
            ax.set_zlabel(label_z)
            ax.set_title(f'Acquisition function for X{i+1}')

        if filename != None:
            plt.savefig(filename)
        else:
            plt.show()

'''