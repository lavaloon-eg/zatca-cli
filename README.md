# ZATCA CLI

`zatca-cli` is a wrapper around the ZATCA e-invoicing SDK that exposes command results in machine-readable JSON format.
It's used by the [KSA Compliance frappe app](https://github.com/lavaloon-eg/ksa_compliance/) to generate CSRs, sign, and
validate invoices.

## Limitations

The CLI works only on Java 11 due to an SDK limitation. The elliptic curve used by the SDK (secp256k1) was removed from 
later Java versions.