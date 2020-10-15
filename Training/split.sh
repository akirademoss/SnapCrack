#!/bin/bash

CURRENTDATE=`date +"%Y-%m-%d_%T"`
DIRNAME = 'SnapCrack_Data_'

if [ ! -d ./train ] 
then
    mkdir -p ./train
fi  
if [ ! -d ./test ] 
then
    mkdir -p ./test
fi   
if [ ! -d ./images_archive ] 
then
    mkdir -p ./images_archive
fi 
if [ ! -d ./data ] 
then
    mkdir -p ./data
fi 

file='train.txt'
while read line; do
echo $line
cp $line ./train
filepath=${line%.*}
cp $filepath.xml ./train
echo $filepath.xml
done < $file

file='test.txt'
while read line; do
echo $line
cp $line ./test
filepath=${line%.*}
cp $filepath.xml ./test
echo $filepath.xml
done < $file

mkdir -p ./images
cp -R ./train ./images
cp -R ./test ./images

mkdir -p ./images_archive/SnapCrack_${CURRENTDATE}
mv -v ./train ./test -t ./images_archive/SnapCrack_${CURRENTDATE}
rm -rf ./train.txt ./test.txt



