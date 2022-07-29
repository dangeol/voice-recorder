# Configuration & Troubleshooting

## Introduction
The Bot is deployed in a Docker container on a Virtual Machine (VM) instance using the following Google Cloud Platform hardware resources:
Machine type e2-micro, “always free tier”.

## Troubleshooting

### Prerequisites

- Being member of the voicerecorder gcloud project. As of 2020-11-08, these are:
  - Chris: cxxxg@cxxxp.me (editor)
  - Sebastian: sxxxl@pxxxo.de (editor)
  - Daniel: dxxx0@gxxxl.com (owner)

- Having the latest [Cloud SDK](https://cloud.google.com/sdk/docs#install_the_latest_cloud_tools_version_cloudsdk_current_version) installed.
- Alternatively, the [Cloud Shell](https://console.cloud.google.com/home/dashboard?cloudshell=true) can be used.

### Scenario 1: The VM is running, but a recording has not been uploaded to the google drive.
This has happened occasionally. During the upload, some random “time out” error has occurred. In this case, the most recent mp3 recording file still resides in the Docker container and should be fetched and then be deleted:

`gcloud compute ssh voicerecorder`\
`docker ps` -> get the container id\
Look into the mp3 directory to see which mp3 file is still there:\
`sudo docker exec -it [container_id] /bin/sh -c 'cd mp3 && ls -lha'`\
Copy the mp3 file and the most recent log to the corresponding folders in the OS of the VM:\
`sudo docker cp [container id]:/usr/app/mp3/[file_name].mp3 /home/mp3/`\
`sudo docker cp [container id]:/usr/app/logs/app.log /home/logs/`\
Important: Remove the mp3 file from the Docker container:\
`sudo docker exec -it [container id] /bin/sh -c 'cd mp3 && rm [file_name].mp3'`

Copy the files from the VM to your local machine:\
`gcloud compute scp --recurse voicerecorder:/home/logs C:\[your_directory]`\
`gcloud compute scp --recurse voicerecorder:/home/mp3 C:\[your_directory]`

Upload the mp3 file to your own or Sirimangalo’s google drive for sharing using the following naming convention: 
`[file_name]_[discord_text_channel_name].mp3`

### Scenario 2: The VM has been shut down.
In the case of a shut down, the VM and subsequently the Docker container need to be restarted.

`gcloud compute instances start voicerecorder`

Start a Docker container that is driven from the image, and mount the volume “vr_app”:\
`docker run -p 88:7777 -d --name vr_vol --mount source=vr_app,target=/usr/app [image]`\
The latest image is (November 2020): `5baa766a123f`

## Local development
The following JAR-file needs to be in your JAVA class path: `log4j-core-2.17.2.jar`

## VM configuration
The configuration of the VM should persist even after a shut down. Some configuration details are outlined below.
- OS: Debian GNU/Linux 10 (buster)
- Docker version: 18.06.1-ce
- A swap space of 1 GB has been allocated in the host. This should be verified by the following command: `sudo swapon --show`
- `sudo ls /var/lib/docker/volumes/vr_app/_data` to see the app’s files
- Some Linux kernel parameters should be optimized. I propose the following:
```
echo 50 | sudo tee /proc/sys/vm/swappiness
echo 50 | sudo tee /proc/sys/vm/vfs_cache_pressure
echo 2 | sudo tee /proc/sys/vm/overcommit_memory
echo 50 | sudo tee /proc/sys/vm/overcommit_ratio
```
[to be continued]
