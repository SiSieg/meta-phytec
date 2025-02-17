FILESEXTRAPATHS:prepend := "${THISDIR}/${BPN}/:"

SRC_URI += "file://fw_env.config"

EMMC_DEV ??= "0"

do_configure:append () {
	sed -i -e 's/@MMCDEV@/${EMMC_DEV}/g' ${WORKDIR}/fw_env.config
}

do_install:append () {
	install -d ${D}${sysconfdir}
	install -m 644 ${WORKDIR}/fw_env.config ${D}${sysconfdir}/fw_env.config
}

FILES:${PN} += "${sysconfdir}/fw_env.config"
