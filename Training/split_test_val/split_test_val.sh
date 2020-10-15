#!/bin/bash

CURRENTDATE=`date +"%Y-%m-%d_%T"`
DIRNAME = 'SnapCrack_Data_'

if [ ! -d ./train ] 
then
    mkdir -p ./train
fi 
if [ ! -d ./validate ] 
then
    mkdir -p ./validate
fi 
if [ ! -d ./test ] 
then
    mkdir -p ./test
fi 


file='train.txt'
while read line; do
echo $line
cp $line ./train
filepath=${line%.*}
cp $filepath.xml ./train
echo $filepath.xml
done < $file

file='validate.txt'
while read line; do
echo $line
cp $line ./validate
filepath=${line%.*}
cp $filepath.xml ./validate
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

mkdir -p ./SnapCrack_${CURRENTDATE}
mv -v ./train ./validate ./test -t ./SnapCrack_${CURRENTDATE}
rm -rf ./train.txt ./validate.txt ./test.txt

