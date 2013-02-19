path=$HOME"/.ssh/id_rsa"


# hello
echo ""
echo "T.A.L.E.S Installer"
echo ""


# asks for the user data

echo "Note: if want to experiment with Tales, you can use the main repo 'git@github.com:calufa/tales-core.git' as the tales-core path."
read -p "1. What's your tales-core repo path: " core
echo ""
echo "Note: there is a sample of tales-templates in the repo 'git@github.com:calufa/tales-templates-sample.git', this sample will get you started,"
echo "as it contains a demo of the scraper instructions also called templates."
read -p "2. What's your tales-templates repo path: " templates
read -p "3. What's your github email address? " email
read -p "4. What's your github username? " name
echo ""
echo "Note: if you submitted 'git@github.com:calufa/tales-templates-sample.git' as the tales-templates repo,"
echo "then you should use the default user (dbUsername) 'root' (no quotes),"
echo "as specified in the config file at this url https://github.com/calufa/tales-templates-sample/blob/master/environments/master.json#L29"
read -p "5. Please specify the mysql username? " user
echo ""
echo "Note: if you submitted 'git@github.com:calufa/tales-templates-sample.git' as the tales-templates repo,"
echo "then you should use the default password (dbPassword) '123456789-ABCDED' (no quotes),"
echo "as specified in the config file at this url https://github.com/calufa/tales-templates-sample/blob/master/environments/master.json#L30"
read -p "6. Please specify the mysql password? " password


# verify info
echo ""
echo "Please verify the submmited information."
echo ""
echo "* tales-core path:" $core
echo "* tales-templates path:" $templates
echo "* github email address:" $email
echo "* github username:" $name
echo "* mysql username:" $user
echo "* mysql password:" $password
echo ""

while true; do
    read -p "7. Is the information summited correct? (options 'yes' or 'no') " answer
    case $answer in
        [yes]* ) break;;
        [no]* ) echo "Please run this script again."; exit ;;
        * ) echo "    Please answer 'yes' or 'no' (no quotes)";;
    esac
done


# lets generate the ssh-key
echo ""
echo "Generating ssh-key..."
echo ""
ssh-keygen -f $path -N "" -t rsa -C $email > /dev/null 2>&1


# prints the key
echo "8. Now copy the ssh-key below and add it to github at 'https://github.com/settings/ssh'. As 'title' you can use 'tales'."
echo ""
echo "~~~~~~~~~~~~~~~ COPY THE TEXT BELLOW THIS COMMENT ~~~~~~~~~~~~~~~"
echo ""
cat $path".pub"
echo ""
echo "~~~~~~~~~~~~~~~~~~~~~~~ END COPYING HERE ~~~~~~~~~~~~~~~~~~~~~~~~"
echo ""
echo "Why do I need to create a private key?"
echo ""
echo "One of the things Tales do, is pull code changes from git and recompile itself."
echo "Tales uses 2 repos, one for the tales-core (where the main logic lives), and the other is for the templates (scraping instructions on how to scrape a page)."
echo "The most probable thing is that you are going to host tales-core and tales-templates privately."
echo "Github doesn't allow arbitrary servers to pull code from private repos without an ssh-key."
echo ""
while true; do
    read -p "Let me know when you are done adding the key to github by typing 'done'? " done
    case $done in
        [done]* ) break;;
        * ) echo "    Please answer 'done' (no quotes)";;
    esac
done
 

# installing dependencies
echo ""
echo ""
echo ""
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
echo "Installing dependencies... this can take a while..."
echo ""
echo ""

echo mysql-server mysql-server/root_password select $password | debconf-set-selections # prevents mysql prompt
echo mysql-server mysql-server/root_password_again select $password | debconf-set-selections # prevents mysql prompt

sudo apt-get -q -y update
sudo apt-get -q -y install mysql-server openjdk-6-jdk openjdk-6-jre-headless openjdk-6-jre-lib build-essential g++ libssl-dev git-core libxml2 libxml2-dev scons ant screen apache2 munin munin-node munin-java-plugins munin-plugins-extra curl
#redis
cd $HOME 
wget http://redis.googlecode.com/files/redis-2.6.8.tar.gz
tar xzf redis-2.6.8.tar.gz
cd redis-2.6.8
make
cd ../ 
mv redis-2.6.8 redis

echo ""
echo ""
echo ""
echo "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"
echo "Finished installing all the dependencies."
echo ""
echo ""
echo ""


# inits git
git config --global user.name $name > /dev/null 2>&1
git config --global user.email $email > /dev/null 2>&1


# cloning repos
echo -e "Host github.com\n\tStrictHostKeyChecking no\n" >> ~/.ssh/config # prevents git for asking "Are you sure you want to continue connecting (yes/no)?"

echo ""
echo "Cloning tales-core from '"$core"'"
git clone $core "tales-core" > /dev/null 2>&1
cd $HOME
cd tales-core/core
ant > /dev/null 2>&1

echo "Cloning tales-templates from '"$templates"'"
cd $HOME
git clone $templates "tales-templates" > /dev/null 2>&1
cd tales-templates/core
ant > /dev/null 2>&1

# setting up mysql passwords
mysql -u $user -p$password -e "GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY '"$password"' WITH GRANT OPTION; GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' IDENTIFIED BY '"$password"' WITH GRANT OPTION; FLUSH PRIVILEGES;"


# iptables

echo ""
echo "*** Note: If you are running on AWS you should allow the following ports: 8080 (web), 3306 (mysql), 8983 (redis), 6379 (solr), 27017 (mongo) at 'https://console.aws.amazon.com/ec2/home?region=us-east-1#s=SecurityGroups'"

# web
iptables -A INPUT -i eth0 -p tcp -m tcp --dport 8080 -j ACCEPT
# mysql
iptables -A INPUT -i eth0 -p tcp -m tcp --dport 3306 -j ACCEPT
# redis
iptables -A INPUT -i eth0 -p tcp -m tcp --dport 8983 -j ACCEPT
# solr
iptables -A INPUT -i eth0 -p tcp -m tcp --dport 6379 -j ACCEPT
# mongo
iptables -A INPUT -i eth0 -p tcp -m tcp --dport 27017 -j ACCEPT


# init.d
echo -e '\n# tales-server\nsudo java -cp /root/tales-core/core/dist/tales-core.jar tales.server.Server' >> /etc/init.d/rc.local


#
echo ""
while true; do
    read -p "Now we need to reboot, should I reboot now? " answer
    case $answer in
        [yes]* ) echo "Bye..."; reboot > /dev/null 2>&1 ; break;;
        [no]* ) echo "Ok, changes will apply when you reboot the server."; exit ;;
        * ) echo "    Please answer 'yes' or 'no' (no quotes)";;
    esac
done
