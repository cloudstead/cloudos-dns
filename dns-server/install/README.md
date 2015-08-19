Standalone cloudos-dns Server
=============================

If you would like your cloudstead to manage DNS via a djbdns server that is *not* running on the cloudstead itself,
you'll need to install this cloudos-server on the system where the djbdns server is running.

You will then configure your cloudsteads to talk to this cloudos-dns server, which will in turn manage the djbdns
server running local to it.

## System requirements

cloudos-dns requires:

   * Java (version 7+)
   * PostgreSQL (version 8+)
   * Redis
   * Kestrel
   * memcached
   * curl
   * uuid

For Ubuntu servers, run `ubuntu_init.sh` to install the required packages.

## Installation

Edit the `install.env` file to configure the installation.

Then run `install_standalone.sh` to install and start the cloudos-dns server.

## Controlling the server

Use the following commands to control the cloudos-dns server:

   * `service cloudos-dns stop`
   * `service cloudos-dns start`
   * `service cloudos-dns restart`
   * `service cloudos-dns status`

## Accessing the server from a cloudstead

The cloudos-dns server only listens on localhost via regular HTTP. 

In order for your cloudsteads to access it, you will need to install a webserver (Apache or 
nginx are good choices), and proxy requests to it. You can also configure your webserver to use HTTPS,
so that DNS management traffic is encrypted.

## Creating accounts

Use the `cdns` utility to add accounts. A single admin account is created at installation time.
The login for the admin account is set in `install.env` with `CLOUDOS_ADMIN_USER` (default is `admin`).
The password for the admin account is `CLOUDOS_ADMIN_PASS` (no default, you must set this in `install.env`) 

Let's say you are setting up a cloudstead named foo.example.com, and your admin password is 'pass123'

    export CLOUDOS_DNS_PASS=pass123
    cdns admin -a admin -d foo -z example.com

The output from the cdns command will show you the password that was generated for the new account.
Alternatively, you can set the password yourself by adding `-P <password>` to the `cdns` command.

When launching your new cloudstead, use the credentials you just created above for the DNS user and password.
For the DNS URL, use the publicly accessible URL that your webserver uses to proxy to cloudos-dns.

## Managing DNS records

Your cloudsteads will manage their DNS records themselves, connecting to cloudos-dns with credentials that 
you've created.

You can also manage DNS records directly with the `cdns` command.
Run `cdns dns -h` to view options for this command.
