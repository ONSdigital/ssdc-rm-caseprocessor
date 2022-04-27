apt-get update
apt-get install -y gnupg
apt-get install -y git
cd ssdc-rm-caseprocessor
gpg --no-default-keyring --keyring trustedkeys.gpg --import codecov_public_key.asc
curl -Os https://uploader.codecov.io/latest/linux/codecov
curl -Os https://uploader.codecov.io/latest/linux/codecov.SHA256SUM
curl -Os https://uploader.codecov.io/latest/linux/codecov.SHA256SUM.sig

  # NOTE: the "|| exit 1"'s are required to stop travis continuing to run subsequent steps even if the integrity checks failed
gpgv codecov.SHA256SUM.sig codecov.SHA256SUM || exit 1
shasum -a 256 -c codecov.SHA256SUM || exit 1

chmod +x codecov
./codecov