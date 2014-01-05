# keepass-converter

a command-line utility to convert KeePass export files to CSV format

## Installation

1. Checkout sources
2. Build with ``lein uberjar``

## Usage

1. Export your passwords from KeePass to XML format
2. Execute 

    $ java -jar keepass-converter-0.1.0-standalone.jar path/to/your.xml

## Options

  -o, --output 1password.csv  Output file in the requested 1Password format
  -i, --interactive           You will be prompt about conversion of every entry in the input file
  -h, --help

## License

Copyright © 2014 Pavel Prokopenko

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
