 #!/usr/bin/env bash

set -e

if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters"
    exit 1
fi

. package/methods.sh || (echo Please be in the package directory while doing this ; exit 1)

ENV=$1
echo Deploying to $ENV
echo Building
build_all $ENV

deploy_package() {
    HOST=$1
    PORT=$2
    USER=$3

    set -x
    scp -P ${PORT} target/*.zip ${USER}@${HOST}:~/doffapp.zip
    ssh -p ${PORT} ${USER}@${HOST} rm -rf /tmp/doffapp
    ssh -p ${PORT} ${USER}@${HOST} sudo mkdir -p /opt/doffapp || (echo Please add ${USER}  ALL=NOPASSWD: ALL to sudo visudo -f /etc/sudoers.d/myOverrides ; exit 1)
    ssh -p ${PORT} ${USER}@${HOST} sudo chown -R ${USER} /opt/doffapp
    ssh -p ${PORT} ${USER}@${HOST} unzip -o doffapp.zip -d /tmp/doffapp
    ssh -p ${PORT} ${USER}@${HOST} chmod 755 /tmp/doffapp/doffapp
    ssh -p ${PORT} ${USER}@${HOST} rsync -a --delete -v /tmp/doffapp/ /opt/doffapp
    ssh -p ${PORT} ${USER}@${HOST} sudo cp -v /opt/doffapp/doffapp /etc/init.d/doffapp
    ssh -p ${PORT} ${USER}@${HOST} sudo service doffapp restart
    ssh -p ${PORT} ${USER}@${HOST} rm -rf /tmp/doffapp
}

if [ $ENV = "test" ]
then
    echo Deploying
    deploy_package 10.14.0.104 22 eirirlar
fi