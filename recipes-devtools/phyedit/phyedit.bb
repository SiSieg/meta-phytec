DESCRIPTION = "phyedit is a tool for am335x to access the ethernet phy's \
                mdio registers."
HOMEPAGE = "http://www.phytec.de"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6"
SECTION = "devel"
PR = "r0"

SRC_URI = "file://phyedit.c"

S = "${WORKDIR}"

do_compile() {
	${CC} ${CFLAGS} ${LDFLAGS} -o phyedit phyedit.c
}

do_install() {
	install -d ${D}${bindir}
	install -m 0755 phyedit ${D}${bindir}
}

FILES:${PN} = "${bindir}"

COMPATIBLE_MACHINE = "ti33x"
