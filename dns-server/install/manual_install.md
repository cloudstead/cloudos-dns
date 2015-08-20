Installing cloudos-dns Manually
===============================


# Install Prerequisites

Install the following software
   * djbdns
   * Java (version 7 - NOT version 8)
   * PostgreSQL (version 8+)
   * PostgreSQL (version 8+)
   * Redis
   * Kestrel
   * memcached
   * curl
   * uuid

# Create user and copy server files

   * Create a user the cloudos-dns server will run as. Typically, this is cloudos-dns
   * In this user's home directory, create a directory named cloudos-dns.
   * Copy the file named cdns and the directories named `target` and `logs` from this directory to the cloudos-dns directory (the one you just created, not the cloudos-dns HOME directory)
   * Create a symlink for cdns in /usr/local/bin
        * `(cd /usr/local/bin && ln -s ~cloudos-dns/cdns)`
   * Copy `install.env` to `~cloudos-dns/.cloudos-dns.env`
   * Edit `~cloudos-dns/.cloudos-dns.env`:
        * Set CLOUDOS_DNS_DB_PASS to be the database user's password (we'll use this below when creating the database user)
        * Set ROOTY_SECRET to be some random data
        * Add the following line: `export SESSION_DATAKEY=--put-some-random-data-here--` (replace this value with a random string at least 10 chars long)
   * Ensure ~cloudos-dns is owned by cloudos-dns:
        * `chown -R cloudos-dns ~cloudos-dns`
   * Copy jrun and jrun-init from this directory to /usr/local/bin
   * Copy `rooty.yml` from this directory to `/etc/rooty.yml`
   * Edit `/etc/rooty.yml` and set the `secret` field to the same value as `ROOTY_SECRET` above

# Set up the database
   
   * Create a database user named ${CLOUDOS_DNS_DB_USER} (set in `install.env`). You may need to run this as the postgres user.
       * `. install.env && createuser ${CLOUDOS_DNS_DB_USER} && echo "ALTER USER ${CLOUDOS_DNS_DB_USER} PASSWORD '"${CLOUDOS_DNS_DB_PASS}"'" | psql -U postgres`
   * Create a database named ${CLOUDOS_DNS_DB_NAME}. You may need to run this as the postgres user.
       * `. install.env && createdb ${CLOUDOS_DNS_DB_NAME}`
   * Populate the cloudos_dns database 
       * `. install.env &&  cat cloudos-dns.sql | PGPASSWORD="${CLOUDOS_DNS_DB_PASS}" psql -U ${CLOUDOS_DNS_DB_USER} ${CLOUDOS_DNS_DB_NAME}`
   * Create the super-admin user
      * Create a bcrypt'ed password: `ADMIN_PASS_BCRYPTED=$(java -cp $(find target -type f -name *.jar) org.cobbzilla.util.security.bcrypt.BCryptUtil 12 some-password)`
      * `PGPASSWORD="${CLOUDOS_DNS_DB_PASS}" psql -U ${CLOUDOS_DNS_DB_USER} -c "insert into dns_account (uuid, ctime, admin, name, hashed_password) VALUES ('put-some-uuid-here', 0, TRUE, '"${CLOUDOS_ADMIN_USER}"', '"${ADMIN_PASS_BCRYPTED}"')" ${CLOUDOS_DNS_DB_NAME}`
        
# Create services

   * Copy `cloudos-dns` and `cloudos-dns-rooty` from this directory to /etc/init.d
   * Edit `/etc/init.d/cloudos-dns` and `cloudos-dns-rooty`. Replace `@@USER@@` with the name of the user that will be running the cloudos-dns server (typically cloudos-dns)

# Start services

`service cloudos-dns start`

`service cloudos-dns-rooty start`

Verify the service is running properly: `curl http://127.0.0.1:4002/api/dns`

If you run into any trouble, check in `~cloudos-dns/cloudos-dns/logs`

# Set up web server front-end

Ideally you'll want Apache or nginx as a front-end webserver, with https support.

For development purposes, you can use an ssh tunnel to simply route all traffic on a particular port to 
cloudos-dns: `ssh -g -R 4003:localhost:4002 $(hostname)`

Now you can access cloudos-dns from the outside world: `curl http://your-hostname:4003/api/dns`
Use the above as the cloudos-dns `base_uri` when setting up a new cloudstead.

#### Common problems

If you get an error like this: `java.security.InvalidKeyException: Illegal key size` then
install the JCE unlimited strength encryption from here:

    http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html
