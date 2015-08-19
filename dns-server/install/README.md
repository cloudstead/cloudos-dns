Standalone cloudos-dns Server
=============================

If you would like your cloudstead to manage DNS via a djbdns server that is *not* running on the cloudstead itself,
you'll need to install this cloudos-server on the system where the djbdns server is running.

You will then configure your cloudsteads to talk to this cloudos-dns server, which will in turn manage the djbdns
server running local to it.

## System requirements

cloudos-dns requires:

   * djbdns
   * Java (version 7+)
   * PostgreSQL (version 8+)
   * Redis
   * Kestrel
   * memcached
   * curl
   * uuid

For Ubuntu servers, run `ubuntu_init.sh` to install the required packages (except djbdns, which we assume
you already have installed and configured). To install djbdns from scratch: http://cr.yp.to/djbdns/install.html

## Installation

Edit the `install.env` file to configure the installation. The only field you that MUST be set 
manually is `CLOUDOS_ADMIN_PASS`.

Run `./install_standalone.sh` to install and start the cloudos-dns server.

## Controlling the server

Use the following commands to control the cloudos-dns server:

   * `service cloudos-dns stop`
   * `service cloudos-dns start`
   * `service cloudos-dns restart`
   * `service cloudos-dns status`

There is a separate service called `cloudos-dns-rooty` that performs the modifications of the djbdns data file.
Because the djbdns data file is only writeable by root, `cloudos-dns-rooty` runs as root, while `cloudos-dns`
provides the REST API and runs as an unprivileged user. You can control the `cloudos-dns-rooty` service too:

   * `service cloudos-dns-rooty stop`
   * `service cloudos-dns-rooty start`
   * `service cloudos-dns-rooty restart`
   * `service cloudos-dns-rooty status`

## Accessing the server from a cloudstead

The cloudos-dns server only listens on localhost via regular HTTP. 

In order for your cloudsteads to access it, you will need to install a webserver (Apache or 
nginx are good choices). Configure your web server to proxy requests to cloudos-dns. 
You should also configure your webserver to use HTTPS, so that DNS management traffic is encrypted.

## Creating accounts

Use the `cdns` utility to add accounts. A single admin account is created at installation time.
The login for the admin account is set in `install.env` with `CLOUDOS_ADMIN_USER` (default is `admin`).
The password for the admin account is `CLOUDOS_ADMIN_PASS` (no default, you must set this in `install.env`) 

Let's say you are setting up a cloudstead named foo.example.com, and your admin password is 'pass123'

    export CLOUDOS_DNS_PASS=pass123
    cdns admin -a admin -d foo -z example.com

The output from the cdns command will show you the password that was generated for the new account.
Alternatively, you can set the password yourself by adding `-P <password>` to the `cdns` command.

The user just created will only be able to read and write DNS records that end with foo.example.com.

When launching your new cloudstead, use the credentials you just created above for the DNS user and password.
For the DNS URL, use the publicly accessible URL that your webserver uses to proxy to cloudos-dns.

## Managing DNS records

Your cloudsteads will manage their DNS records themselves, connecting to cloudos-dns with credentials that 
you've created.

You can also manage DNS records directly with the `cdns` command.
Run `cdns dns -h` to view options for this command.

#### Examples

Create an A record that indicates foo.example.com should resolve to 10.2.3.4

    cdns dns -a admin -o add -r a -f foo.example.com -v 10.2.3.4

Create an MX record that indicates mail to foo.example.com should go to mx.foo.example.com, with a weight of 10

    cdns dns -a admin -o add -r mx -f foo.example.com -v mx.foo.example.com -O "rank=10"

Remove all records whose FQDN ends with foo.example.com

    cdns dns -a admin -o remove -S foo.example.com

Remove all CNAME records whose FQDN ends with foo.example.com

    cdns dns -a admin -o remove -r cname -S foo.example.com

List all records whose FQDN ends with foo.example.com

    cdns dns -a admin -o list -S foo.example.com

List all MX records whose FQDN ends with foo.example.com

    cdns dns -a admin -o list -r mx -S foo.example.com
