# Overview

This file contains release notes between tagged versions of the product. Please update this file with your pull
requests so that whoever deploys a given version can file the relevant changes under the corresponding version.

Add changes to the "Pending Changes" section. Once you create a version (and tag it), move the pending changes
to a section with the version name.

## Pending Changes

* Update ZATCA SDK from 238-R3.3.4 to 238-R3.3.5

## 2.1.1

* Update SDK schematrons to 238-R3.3.4. This should've been done with the SDK update

## 2.1.0

* Update version (`-v`) to expose exact version in the payload instead of just a display string. This allows clients to
  parse and check it to decide whether certain features are supported
* Improve error message for unknown commands
* Enhance validate command to expose more details
  * We now expose structured dictionaries of (code, error) and (code, warning) for both errors and warnings
* Update ZATCA SDK from 238-R3.3.1 to 238-R3.3.4 

## 2.0.1

* Fix simulation environment support by adding `-s` (`--simulation`) flag to CSR generation.

## 2.0.0

* Rename to `zatca-cli`

## 1.2.0

* Add validate command

## 1.1.0

* Initial release