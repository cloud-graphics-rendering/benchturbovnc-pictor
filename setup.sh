#/bin/bash
sudo apt-get install cmake libx11-dev libpam0g-dev

folder=/opt/libjpeg-turbo
if [ -e "$folder" ] then
	echo "libjpeg-turbo exits!"
else
	wget https://sourceforge.net/projects/libjpeg-turbo/files/2.0.0/libjpeg-turbo-official_2.0.0_amd64.deb
	sudo dpkg -i libjpeg-turbo-official_2.0.0_amd64.deb
	echo "configuration Done!"
fi
