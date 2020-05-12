# SnapCrack 
SnapCrack is an Android application that is capable of crack detection and localization in real time.  This application leverages basic object detection algorithms to detect cracks.  After a crack has been detected, an image will be stored and sent to a database where it will be stored until it is viewed in a web portal.  This repository contains this application aswell as tools for modifying the dataset, training the object detector. 

## Summary of Repository

**1.) SnapCrack_App**

The [SnapCrack_App](https://git.ece.iastate.edu/sd/sdmay20-18/-/tree/master/SnapCrack_App) module contains the Snapcrack Android Application.

**2.) Training**

The [Training](https://git.ece.iastate.edu/sd/sdmay20-18/tree/master/Training) module contains installation steps and tutorials for training a custom object detector.  

**3.) Server**

The [Server](https://git.ece.iastate.edu/sd/sdmay20-18/-/tree/master/Server) module contains basic testing for sending images to a server and database.

**4.) snapcrack**

The [snapcrack](https://git.ece.iastate.edu/sd/sdmay20-18/-/tree/master/snapcrack) module contains the code for the React web portal.

## Additional Files for Training

the SnapCrack dataset used to train the model can be found [here](https://drive.google.com/file/d/1Nl70wNflgs3ek0_dapJ1MqloVYuOc9Os/view?usp=sharing), aswell as the corresponding tfrecord files can be found [here](https://drive.google.com/open?id=1JUVAeQKUlf1-SQaz1TxZf-O4WlaXLZvf).  These can be used or re-generated to create a new model.
