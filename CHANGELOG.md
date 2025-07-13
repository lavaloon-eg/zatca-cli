# Overview

This file contains release notes between tagged versions of the product. Please update this file with your pull
requests so that whoever deploys a given version can file the relevant changes under the corresponding version.

Add changes to the "Pending Changes" section. Once you create a version (and tag it), move the pending changes
to a section with the version name.

## Pending Changes

## 2.8.0

* Update ZATCA SDK FROM 238-R3.4.1 to 238-R3.4.2

## 2.7.0

* Update ZATCA SDK FROM 238-R3.3.9 to 238-R3.4.1

## 2.6.0

* Fix Invoice validation failure due to difference between `IssueDate` & `SigningTime`
  * The CLI used the local time at the server timezone (typically `UTC`) for signing time. However, issue date/time
    come from ERPNext (typically KSA time, which is UTC+3). This would result in a discrepancy, since both fields should be
    in the same timezone. Additionally, validation would fail for invoices from 12 AM to 3 AM because it'd be the next
    day in KSA (compared to UTC) and the SDK would complain about issue date being in the future (business rule: `BR-KSA-04`)
  * CLI timezone is now explicitly set to `Asia/Riyadh` timezone regardless of the server time zone

## 2.5.0

* Add preliminary PDF/A-3B support using Spire.PDF free
  * Spire.PDF free is limited to 10-page PDFs, which should work fine for invoices
  * We'll look into open-source alternatives for future-proofing

## 2.4.0

* Update ZATCA SDK from 238-R3.3.6 to 238-R3.3.9

## 2.3.0

* Update ZATCA SDK from 238-R3.3.5 to 238-R3.3.6

## 2.2.0

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
