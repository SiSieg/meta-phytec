DESCRIPTION =  "Kernel drivers for the PowerVR SGX chipset found in the TI SoCs"
HOMEPAGE = "https://git.ti.com/graphics/omap5-sgx-ddk-linux"
LICENSE = "MIT | GPL-2.0-only"
LIC_FILES_CHKSUM = "file://eurasia_km/README;beginline=13;endline=22;md5=74506d9b8e5edbce66c2747c50fcef12"

inherit module

COMPATIBLE_MACHINE = "ti33x|ti43x|omap-a15"

MACHINE_KERNEL_PR:append = "2"
PR = "${MACHINE_KERNEL_PR}"

INHIBIT_PACKAGE_STRIP = "1"

PACKAGE_ARCH = "${MACHINE_ARCH}"

DEPENDS = "virtual/kernel"

PROVIDES = "omapdrm-pvr"

RPROVIDES:${PN} = "omapdrm-pvr"
RREPLACES:${PN} = "omapdrm-pvr"
RCONFLICTS:${PN} = "omapdrm-pvr"

BRANCH = "ti-img-sgx/${PV}/k4.14"

SRC_URI = "git://git.ti.com/graphics/omap5-sgx-ddk-linux.git;protocol=git;branch=${BRANCH}"
SRC_URI += " \
	file://0001-srvkm-env-linux-Check-whether-Soc-supports-SGX.patch \
	file://0001-srvkm-env-linux-osfunc.c-fix-gcc8-stringop-truncatio.patch \
	file://0001-buildvars.mk-pass-Wno-cast-function-type.patch \
"
S = "${WORKDIR}/git"


SRCREV = "76da7d73976f0a5dc04fdc84a3af899d6c2b1fe2"

TARGET_PRODUCT_omap-a15 = "jacinto6evm"
TARGET_PRODUCT:ti33x = "ti335x"
TARGET_PRODUCT_ti43x = "ti437x"

PARALLEL_MAKE = ""

EXTRA_OEMAKE += 'KERNELDIR="${STAGING_KERNEL_DIR}" TARGET_PRODUCT=${TARGET_PRODUCT}'


do_compile:prepend() {
    cd ${S}/eurasia_km/eurasiacon/build/linux2/omap_linux
}

do_install() {
    make -C ${STAGING_KERNEL_DIR} SUBDIRS=${B}/eurasia_km/eurasiacon/binary2_omap_linux_release/target/kbuild INSTALL_MOD_PATH=${D} PREFIX=${STAGING_DIR_HOST} modules_install
}
