#/bin/bash
if [ -e "/opt/TurboVNC" ]
then
	echo "removing orininal turbovnc"
	sudo dpkg -r turbovnc
fi

if [ -e "./turbovnc_2.1.91_amd64.deb" ]
then
	sudo dpkg -i ./turbovnc_2.1.91_amd64.deb
	echo "Install TurboVNC Done!"
else
	rm ./cmake_build -rf
	mkdir ./cmake_build
	cd cmake_build
	cmake -G"Unix Makefiles" ../
	make -j8
	sudo make deb
	echo "Installing TurboVNC .."
	sudo make install
	mv ./turbovnc_2.1.91_amd64.deb ../
	echo "Install TurboVNC Done!"
	echo "Pleae install this turbovnc_2.1.91_amd64.deb to your client machine..."
fi
