# Copyright (C) 2017 PHYTEC Messtechnik GmbH,
# Author: Wadim Egorov <w.egorov@phytec.de>

inherit phygittag
inherit buildinfo
include linux-common.inc

GIT_URL = "git://git.phytec.de/${PN}"
SRC_URI = "${GIT_URL};branch=${BRANCH}"

PR = "${INC_PR}.0"

RDEPENDS_kernel-modules_rk3288 += "cryptodev-module"

# NOTE: PV must be in the format "x.y.z-.*". It cannot begin with a 'v'.
# NOTE: Keep version in filename in sync with commit id!
SRCREV = "e2deb7324ab4c66a35edc2fe6805b250a215940f"

S = "${WORKDIR}/git"

COMPATIBLE_MACHINE  = "^("
COMPATIBLE_MACHINE .= "phycore-rk3288-3"
COMPATIBLE_MACHINE .= ")$"
