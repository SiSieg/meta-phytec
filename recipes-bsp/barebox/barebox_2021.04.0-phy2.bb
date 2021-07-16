# Copyright (C) 2018 PHYTEC Messtechnik GmbH,
# Author: Stefan Riedmueller <s.riedmueller@phytec.de>

inherit phygittag
inherit buildinfo
require barebox.inc
inherit barebox-environment-2

include barebox-secureboot.inc
include barebox-protectionshield.inc
include barebox-boot-scripts.inc

GIT_URL = "git://git.phytec.de/barebox"
SRC_URI = "${GIT_URL};branch=${BRANCH}"
SRC_URI_append = "file://0001-of_dump-Add-a-simple-node-check-up.patch"

S = "${WORKDIR}/git"

PR = "${INC_PR}.0"

# NOTE: Keep version in filename in sync with commit id!
SRCREV = "85714efb09754c01c6a08a7e7fc2037fded4a69e"

python do_env_append() {
    env_add(d, "nv/allow_color", "false\n")
    env_add(d, "nv/linux.bootargs.base", "consoleblank=0\n")
    env_add(d, "nv/linux.bootargs.rootfs", "rootwait ro fsck.repair=yes\n")
    env_add(d, "bin/far",
"""#!/bin/sh
# barebox script far (="Fetch And Reset"):
#
# The script is useful for a rapid compile and execute development cycle. If
# the deployment directory of yocto is the root directory of the tftp server
# (e.g. use a bind mount), you can fetch and execute a newly compiled barebox
# with this script.

cp /mnt/tftp/barebox.bin /dev/ram0
if [ $? != 0 ]; then
    echo "Error: Cannot fetch file \"barebox.bin\" from host!"
else
    go /dev/ram0
fi
""")

    env_add(d, "expansions/imx6-phytec-check-bus-nodepath",
"""bus="bus"
of_dump -e /soc/$bus@2000000

if [ $? != 0 ]; then
    echo "Changing node-name bus to aips-bus"
    bus="aips-bus"
fi
""")
}

python do_env_append_mx6() {
    kernelname = d.getVar("KERNEL_IMAGETYPE", True)
    if "secureboot" in d.getVar("DISTRO_FEATURES", True):
        kernelname = "fitImage"
    sdid = "2"
    emmcid = None
    dhcp_vendor = "phyFLEX-i.MX6"

    if "phyboard" in d.getVar("SOC_FAMILY"):
        sdid = "0"
        emmcid = "3"
        dhcp_vendor = "phyCORE-i.MX6"

    if "phycard" in d.getVar("SOC_FAMILY"):
        dhcp_vendor = "phyCARD-i.MX6"

    env_add_boot_scripts(d, kernelname, sdid, emmcid)
    env_add_bootchooser(d)

    env_add(d, "expansions/imx6qdl-mira-enable-lvds",
"""of_fixup_status /ldb/lvds-channel@0
of_fixup_status /soc/$bus@2100000/i2c@21a0000/touchctrl@44
""")
    env_add(d, "expansions/imx6qdl-nunki-enable-lvds",
"""of_fixup_status /ldb/lvds-channel@0
of_fixup_status /soc/$bus@2100000/i2c@21a0000/touchctrl@44
""")
    env_add(d, "expansions/imx6qdl-mira-peb-eval-01",
"""of_fixup_status /gpio-keys
of_fixup_status /user-leds
of_property -s -f -e $global.bootm.oftree /soc/$bus@2100000/serial@21ec000 pinctrl-0 </soc/$bus@2000000/pinctrl@20e0000/uart3grp>
""")
    env_add(d, "expansions/imx6qdl-phytec-lcd",
"""#!/bin/sh
of_fixup_status /panel-lcd
of_fixup_status /ldb/lvds-channel@0
of_fixup_status /soc/$bus@2100000/i2c@21a4000/polytouch@38
""")
    env_add(d, "expansions/imx6qdl-phytec-lcd-res",
"""#!/bin/sh
of_fixup_status /panel-lcd
of_fixup_status /ldb/lvds-channel@0
of_fixup_status /soc/$bus@2100000/i2c@21a4000/touchctrl@41
""")
    env_add(d, "expansions/imx6qdl-phytec-lcd-018-peb-av-02",
"""of_fixup_status /panel-lcd
of_fixup_status /display@di0
of_fixup_status /soc/$bus@2100000/i2c@21a0000/polytouch@38
""")
    env_add(d, "expansions/imx6qdl-phytec-lcd-018-peb-av-02-res",
"""of_fixup_status /panel-lcd
of_fixup_status /display@di0
of_fixup_status /soc/$bus@2100000/i2c@21a0000/touchctrl@44
""")
    env_add(d, "expansions/imx6qdl-phytec-peb-wlbt-05",
"""#!/bin/sh
of_fixup_status /soc/$bus@2100000/mmc@2198000
of_fixup_status /regulator-wl-en
of_fixup_status -d /gpio-keys
of_fixup_status /soc/$bus@2100000/serial@21ec000/bluetooth
of_fixup_status -d /user-leds
of_property -s -f -e $global.bootm.oftree /soc/$bus@2100000/serial@21ec000 pinctrl-0 </soc/$bus@2000000/pinctrl@20e0000/uart3grp_bt>
""")
    env_add(d, "expansions/dt-overlays",
"""#!/bin/sh

path="$global.overlays.path"

if [ -e ${path}/select ] ; then
        readf ${path}/select global.overlays.select
fi

for o in $global.overlays.select ; do
        if [ -e ${path}${o} ] ; then
            echo "Add ${path}${o} overlay"
            of_overlay ${path}${o}
        fi
done
""")
    env_add(d, "nv/overlays.select", "")
    env_add(d, "nv/dev.eth0.mode", "static")
    env_add(d, "nv/dev.eth0.ipaddr", "192.168.3.11")
    env_add(d, "nv/dev.eth0.netmask", "255.255.255.0")
    env_add(d, "nv/net.gateway", "192.168.3.10")
    env_add(d, "nv/dev.eth0.serverip", "192.168.3.10")
    env_add(d, "nv/dev.eth0.linux.devname", "eth0")
    env_add(d, "nv/dhcp.vendor_id", "barebox-{}".format(dhcp_vendor))
}

python do_env_append_phyflex-imx6() {
    env_add(d, "config-expansions",
"""#!/bin/sh

. /env/expansions/imx6-phytec-check-bus-nodepath

#use this expansion when a capacitive touchscreen is connected
. /env/expansions/imx6qdl-phytec-lcd

#use this expansion when a resisitive touchscreen is connected
#. /env/expansions/imx6qdl-phytec-lcd-res

# imx6qdl-phytec-lcd: 7" display (AC158 / AC138)
of_property -s -f "/panel-lcd" compatible "edt,etm0700g0edh6"

# imx6qdl-phytec-lcd: 7" display (AC104)
#of_property -s -f "/panel-lcd" compatible "edt,etm0700g0dh6"

# imx6qdl-phytec-lcd: 5.7" display (AC103)
#of_property -s -f "/panel-lcd" compatible "edt,etmv570g2dhu"

# imx6qdl-phytec-lcd: 4.3" display (AC102)
#of_property -s -f "/panel-lcd" compatible "edt,etm0430g0dh6"

# imx6qdl-phytec-lcd: 3.5" display (AC167 / AC101)
#of_property -s -f "/panel-lcd" compatible "edt,etm0350g0dh6"
""")
    # Enable 32 bit color depth for framebuffer emulation on phyFLEX-CarrierBoard
    env_add(d, "nv/linux.bootargs.fb", "imxdrm.legacyfb_depth=32\n");
}

python do_env_append_phyboard-mira-imx6() {
    env_add(d, "config-expansions",
"""#!/bin/sh

. /env/expansions/imx6-phytec-check-bus-nodepath

. /env/expansions/imx6qdl-mira-peb-eval-01
#. /env/expansions/imx6qdl-mira-enable-lvds
#. /env/expansions/imx6qdl-phytec-peb-wlbt-05

#use this expansion when a capacitive touchscreen is connected
#. /env/expansions/imx6qdl-phytec-lcd-018-peb-av-02

#use this expansion when a resisitive touchscreen is connected
#. /env/expansions/imx6qdl-phytec-lcd-018-peb-av-02-res

# imx6qdl-phytec-lcd: 7" display (AC158 / AC138)
#of_property -s -f "/panel-lcd" compatible "edt,etm0700g0edh6"

# imx6qdl-phytec-lcd: 7" display (AC104)
#of_property -s -f "/panel-lcd" compatible "edt,etm0700g0dh6"

# imx6qdl-phytec-lcd: 5.7" display (AC103)
#of_property -s -f "/panel-lcd" compatible "edt,etmv570g2dhu"

# imx6qdl-phytec-lcd: 4.3" display (AC102)
#of_property -s -f "/panel-lcd" compatible "edt,etm0430g0dh6"

# imx6qdl-phytec-lcd: 3.5" display (AC167 / AC101)
#of_property -s -f "/panel-lcd" compatible "edt,etm0350g0dh6"
""")
    # Enable 32 bit color depth for framebuffer emulation on phyBOARD-Mira
    env_add(d, "nv/linux.bootargs.fb", "imxdrm.legacyfb_depth=32\n");
}

python do_env_append_phyboard-nunki-imx6() {
    env_add(d, "config-expansions",
"""#!/bin/sh

. /env/expansions/imx6-phytec-check-bus-nodepath

#. /env/expansions/imx6qdl-nunki-enable-lvds

#use this expansion when a capacitive touchscreen is connected
#. /env/expansions/imx6qdl-phytec-lcd-018-peb-av-02

#use this expansion when a resisitive touchscreen is connected
#. /env/expansions/imx6qdl-phytec-lcd-018-peb-av-02-res

# imx6qdl-phytec-lcd: 7" display (AC158 / AC138)
#of_property -s -f "/panel-lcd" compatible "edt,etm0700g0edh6"

# imx6qdl-phytec-lcd: 7" display (AC104)
#of_property -s -f "/panel-lcd" compatible "edt,etm0700g0dh6"

# imx6qdl-phytec-lcd: 5.7" display (AC103)
#of_property -s -f "/panel-lcd" compatible "edt,etmv570g2dhu"

# imx6qdl-phytec-lcd: 4.3" display (AC102)
#of_property -s -f "/panel-lcd" compatible "edt,etm0430g0dh6"

# imx6qdl-phytec-lcd: 3.5" display (AC167 / AC101)
#of_property -s -f "/panel-lcd" compatible "edt,etm0350g0dh6"
""")
    # Enable VM-016 by default on phyBOARD-Nunki
    env_add(d, "nv/overlays.select", "imx6-vm010-bw-0.dtbo\n");
    # Enable 32 bit color depth for framebuffer emulation on phyBOARD-Nunki
    env_add(d, "nv/linux.bootargs.fb", "imxdrm.legacyfb_depth=32\n");
}

python do_env_append_phyflex-imx6-4() {
    env_add(d, "nv/linux.bootargs.cma", "cma=256M\n")
}

python do_env_append_phyboard-mira-imx6-4() {
    env_add(d, "nv/linux.bootargs.cma", "cma=64M\n")
}

python do_env_append_phyboard-mira-imx6-6() {
    env_add(d, "config-expansions",
"""#!/bin/sh

. /env/expansions/imx6qdl-mira-peb-eval-01
#. /env/expansions/imx6qdl-mira-enable-lvds
. /env/expansions/imx6qdl-phytec-peb-wlbt-05

#use this expansion when a capacitive touchscreen is connected
#. /env/expansions/imx6qdl-phytec-lcd-018-peb-av-02

#use this expansion when a resisitive touchscreen is connected
#. /env/expansions/imx6qdl-phytec-lcd-018-peb-av-02-res

# imx6qdl-phytec-lcd: 7" display (AC158 / AC138)
#of_property -s -f "/panel-lcd" compatible "edt,etm0700g0edh6"

# imx6qdl-phytec-lcd: 7" display (AC104)
#of_property -s -f "/panel-lcd" compatible "edt,etm0700g0dh6"

# imx6qdl-phytec-lcd: 5.7" display (AC103)
#of_property -s -f "/panel-lcd" compatible "edt,etmv570g2dhu"

# imx6qdl-phytec-lcd: 4.3" display (AC102)
#of_property -s -f "/panel-lcd" compatible "edt,etm0430g0dh6"

# imx6qdl-phytec-lcd: 3.5" display (AC167 / AC101)
#of_property -s -f "/panel-lcd" compatible "edt,etm0350g0dh6"
""")
}

python do_env_append_phyboard-mira-imx6-10() {
    env_add(d, "config-expansions",
"""#!/bin/sh

. /env/expansions/imx6qdl-mira-peb-eval-01
#. /env/expansions/imx6qdl-mira-enable-lvds
. /env/expansions/imx6qdl-phytec-peb-wlbt-05

#use this expansion when a capacitive touchscreen is connected
#. /env/expansions/imx6qdl-phytec-lcd-018-peb-av-02

#use this expansion when a resisitive touchscreen is connected
#. /env/expansions/imx6qdl-phytec-lcd-018-peb-av-02-res

# imx6qdl-phytec-lcd: 7" display (AC158 / AC138)
#of_property -s -f "/panel-lcd" compatible "edt,etm0700g0edh6"

# imx6qdl-phytec-lcd: 7" display (AC104)
#of_property -s -f "/panel-lcd" compatible "edt,etm0700g0dh6"

# imx6qdl-phytec-lcd: 5.7" display (AC103)
#of_property -s -f "/panel-lcd" compatible "edt,etmv570g2dhu"

# imx6qdl-phytec-lcd: 4.3" display (AC102)
#of_property -s -f "/panel-lcd" compatible "edt,etm0430g0dh6"

# imx6qdl-phytec-lcd: 3.5" display (AC167 / AC101)
#of_property -s -f "/panel-lcd" compatible "edt,etm0350g0dh6"
""")
}

python do_env_append_phyboard-mira-imx6-11() {
    env_add(d, "config-expansions",
"""#!/bin/sh

. /env/expansions/imx6qdl-mira-peb-eval-01
#. /env/expansions/imx6qdl-mira-enable-lvds
#. /env/expansions/imx6qdl-phytec-peb-wlbt-05

#use this expansion when a capacitive touchscreen is connected
. /env/expansions/imx6qdl-phytec-lcd-018-peb-av-02

#use this expansion when a resisitive touchscreen is connected
#. /env/expansions/imx6qdl-phytec-lcd-018-peb-av-02-res

# imx6qdl-phytec-lcd: 7" display (AC158 / AC138)
of_property -s -f "/panel-lcd" compatible "edt,etm0700g0edh6"

# imx6qdl-phytec-lcd: 7" display (AC104)
#of_property -s -f "/panel-lcd" compatible "edt,etm0700g0dh6"

# imx6qdl-phytec-lcd: 5.7" display (AC103)
#of_property -s -f "/panel-lcd" compatible "edt,etmv570g2dhu"

# imx6qdl-phytec-lcd: 4.3" display (AC102)
#of_property -s -f "/panel-lcd" compatible "edt,etm0430g0dh6"

# imx6qdl-phytec-lcd: 3.5" display (AC167 / AC101)
#of_property -s -f "/panel-lcd" compatible "edt,etm0350g0dh6"
""")
}

python do_env_append_phyboard-mira-imx6-13() {
    env_add(d, "config-expansions",
"""#!/bin/sh

#. /env/expansions/imx6qdl-mira-enable-lvds
#. /env/expansions/imx6qdl-phytec-peb-wlbt-05

#use this expansion when a capacitive touchscreen is connected
. /env/expansions/imx6qdl-phytec-lcd-018-peb-av-02

#use this expansion when a resisitive touchscreen is connected
#. /env/expansions/imx6qdl-phytec-lcd-018-peb-av-02-res

# imx6qdl-phytec-lcd: 7" display (AC158 / AC138)
#of_property -s -f "/panel-lcd" compatible "edt,etm0700g0edh6"

# imx6qdl-phytec-lcd: 7" display (AC104)
of_property -s -f "/panel-lcd" compatible "edt,etm0700g0dh6"

# imx6qdl-phytec-lcd: 5.7" display (AC103)
#of_property -s -f "/panel-lcd" compatible "edt,etmv570g2dhu"

# imx6qdl-phytec-lcd: 4.3" display (AC102)
#of_property -s -f "/panel-lcd" compatible "edt,etm0430g0dh6"

# imx6qdl-phytec-lcd: 3.5" display (AC167 / AC101)
#of_property -s -f "/panel-lcd" compatible "edt,etm0350g0dh6"
""")
}

python do_env_append_phyboard-mira-imx6-15() {
    env_add(d, "nv/linux.bootargs.cma", "cma=64M\n")
}

#Enviroment changes for RAUC
python do_env_append_phyboard-mira-imx6-3() {
    env_add_rauc_nand_boot_scripts(d)
}

python do_env_append_phyboard-mira-imx6-13() {
    env_add_rauc_nand_boot_scripts(d)
}

do_deploy_prepend_mx6ul() {
    if [ -e ${B}/scripts/bareboximd ]; then
        bbnote "Adding CRC32 checksum to barebox Image Metadata"
        ${B}/scripts/bareboximd -c ${B}/${BAREBOX_BIN}
    fi
}

python do_env_append_mx6ul() {
    kernelname = d.getVar("KERNEL_IMAGETYPE", True)
    if "secureboot" in d.getVar("DISTRO_FEATURES", True):
        kernelname = "fitImage"
    sdid = "0"
    emmcid = "1"

    env_add_boot_scripts(d, kernelname, sdid, emmcid)
    env_add_bootchooser(d)

    env_add(d, "nv/dev.eth0.mode", "static")
    env_add(d, "nv/dev.eth0.ipaddr", "192.168.3.11")
    env_add(d, "nv/dev.eth0.netmask", "255.255.255.0")
    env_add(d, "nv/net.gateway", "192.168.3.10")
    env_add(d, "nv/dev.eth0.serverip", "192.168.3.10")
    env_add(d, "nv/dev.eth0.linux.devname", "eth0")
    env_add(d, "nv/dhcp.vendor_id", "phytec")

    env_add(d, "nv/boot.watchdog_timeout", "60s");
}

python do_env_append_phyboard-segin-imx6ul() {
    env_add(d, "config-expansions",
"""#!/bin/sh

. /env/expansions/imx6-phytec-check-bus-nodepath

. /env/expansions/imx6ul-phytec-segin-peb-eval-01
#use this expansion when a capacitive touchscreen is connected
#. /env/expansions/imx6ul-phytec-segin-peb-av-02
#use this expansion when a resisitive touchscreen is connected
#. /env/expansions/imx6ul-phytec-segin-peb-av-02-res

#use this expansion when peb-wlbt-05 adapter is connected
#. /env/expansions/imx6ul-phytec-peb-wlbt-05

#use this bootarg if the VM010 Color is connected
#. /env/expansions/imx6ul-phytec-vm010-col

#use this bootarg if the VM010 Monochrome is connected
#. /env/expansions/imx6ul-phytec-vm010-bw

#use this bootarg if the VM011 Color is connected
#. /env/expansions/imx6ul-phytec-vm011-col

#use this bootarg if the VM011 Monochrome is connected
#. /env/expansions/imx6ul-phytec-vm011-bw

#use this bootarg if the VM009 is connected
#. /env/expansions/imx6ul-phytec-vm009
""")
    env_add(d, "expansions/imx6ul-phytec-segin-peb-eval-01",
"""
of_fixup_status /gpio-keys
of_fixup_status /user-leds
""")
    env_add(d, "expansions/imx6ul-phytec-segin-peb-av-02",
"""
of_fixup_status /soc/$bus@2100000/lcdif@21c8000/
of_fixup_status /panel-lcd
of_fixup_status /backlight
of_fixup_status /regulator-backlight-en
of_fixup_status /soc/$bus@2100000/i2c@21a0000/edt-ft5x06@38
of_fixup_status /soc/$bus@2000000/pwm@2088000/
""")
    env_add(d, "expansions/imx6ul-phytec-segin-peb-av-02-res",
"""
of_fixup_status /soc/$bus@2100000/lcdif@21c8000/
of_fixup_status /panel-lcd
of_fixup_status /backlight
of_fixup_status /regulator-backlight-en
of_fixup_status /soc/$bus@2100000/i2c@21a0000/touchscreen@44
of_fixup_status /soc/$bus@2000000/pwm@2088000/
""")
    env_add(d, "expansions/imx6ul-phytec-peb-wlbt-05",
"""#!/bin/sh
of_fixup_status /soc/$bus@2100000/mmc@2194000
of_fixup_status -d /soc/$bus@2100000/adc@2198000
of_fixup_status /regulator-wl-en
of_fixup_status /soc/$bus@2100000/serial@21e8000
of_fixup_status -d /soc/$bus@2000000/spba-bus@2000000/spi@2010000
of_fixup_status -d /user-leds
""")
    env_add(d, "expansions/imx6ul-phytec-vm010-col",
"""#!/bin/sh
CAM_PATH="/soc/bus@2100000/i2c@21a0000/cam0@48"
ENDPOINT_PATH="/soc/bus@2100000/i2c@21a0000/cam0@48/port/endpoint"

of_property -f -s ${CAM_PATH} compatible "aptina,mt9v024"
of_property -f -d ${CAM_PATH} assigned-clocks
of_property -f -d ${CAM_PATH} assigned-clock-parents
of_property -f -d ${CAM_PATH} assigned-clock-rates

of_fixup_status ${CAM_PATH}
""")
    env_add(d, "expansions/imx6ul-phytec-vm010-bw",
"""#!/bin/sh
CAM_PATH="/soc/bus@2100000/i2c@21a0000/cam0@48"
ENDPOINT_PATH="/soc/bus@2100000/i2c@21a0000/cam0@48/port/endpoint"

of_property -f -s ${CAM_PATH} compatible "aptina,mt9v024m"
of_property -f -d ${CAM_PATH} assigned-clocks
of_property -f -d ${CAM_PATH} assigned-clock-parents
of_property -f -d ${CAM_PATH} assigned-clock-rates

of_fixup_status ${CAM_PATH}
""")
    env_add(d, "expansions/imx6ul-phytec-vm011-col",
"""#!/bin/sh
CAM_PATH="/soc/bus@2100000/i2c@21a0000/cam0@48"
ENDPOINT_PATH="/soc/bus@2100000/i2c@21a0000/cam0@48/port/endpoint"

of_property -f -s ${CAM_PATH} compatible "aptina,mt9p006"
of_property -f -d ${ENDPOINT_PATH} link-frequencies
of_property -f -s ${ENDPOINT_PATH} input-clock-frequency <50000000>
of_property -f -s ${ENDPOINT_PATH} pixel-clock-frequency <50000000>
of_property -f -s ${ENDPOINT_PATH} pclk-sample <0>

of_fixup_status ${CAM_PATH}
""")
    env_add(d, "expansions/imx6ul-phytec-vm011-bw",
"""#!/bin/sh
CAM_PATH="/soc/bus@2100000/i2c@21a0000/cam0@48"
ENDPOINT_PATH="/soc/bus@2100000/i2c@21a0000/cam0@48/port/endpoint"

of_property -f -s ${CAM_PATH} compatible "aptina,mt9p006m"
of_property -f -d ${ENDPOINT_PATH} link-frequencies
of_property -f -s ${ENDPOINT_PATH} input-clock-frequency <50000000>
of_property -f -s ${ENDPOINT_PATH} pixel-clock-frequency <50000000>
of_property -f -s ${ENDPOINT_PATH} pclk-sample <0>

of_fixup_status ${CAM_PATH}
""")
    env_add(d, "expansions/imx6ul-phytec-vm009",
"""#!/bin/sh
CAM_PATH="/soc/bus@2100000/i2c@21a0000/cam0@48"
ENDPOINT_PATH="/soc/bus@2100000/i2c@21a0000/cam0@48/port/endpoint"

of_property -f -s ${CAM_PATH} compatible "micron,mt9m111"
of_property -f -s ${CAM_PATH} clock-names "mclk"
of_property -f -s ${CAM_PATH} phytec,invert-pixclk
of_property -f -s ${CAM_PATH} phytec,allow-10bit
of_property -f -d ${ENDPOINT_PATH} link-frequencies

of_fixup_status ${CAM_PATH}
""")

    env_add_rauc_nand_boot_scripts(d, nandflashsize=512)
}

#No RAUC support for the low-cost Segin due to small NAND
python do_env_append_phyboard-segin-imx6ul-3() {
    env_rm(d, "boot/system0")
    env_rm(d, "boot/system1")
    env_rm_bootchooser(d)
    env_rm_rauc_nand_boot_scripts(d)

    #Default CMA size (128 MB) is too big for the 256 MB RAM so it has to be
    #reduced to 64 MB.
    env_add(d, "nv/linux.bootargs.cma", "cma=64M\n")
}

python do_env_append_phyboard-segin-imx6ul-5() {
    env_rm(d, "config-expansions")
    env_add(d, "config-expansions",
"""#!/bin/sh

. /env/expansions/imx6ul-phytec-segin-peb-eval-01
#use this expansion when a capacitive touchscreen is connected
#. /env/expansions/imx6ul-phytec-segin-peb-av-02
#use this expansion when a resisitive touchscreen is connected
#. /env/expansions/imx6ul-phytec-segin-peb-av-02-res

#use this expansion when peb-wlbt-05 adapter is connected
#. /env/expansions/imx6ul-phytec-peb-wlbt-05

#use this bootarg when the VM010 Color is connected
#nv linux.bootargs.mt9v022="mt9v022.sensor_type=color"
""")
}

python do_env_append_phyboard-segin-imx6ul-6() {
    env_add(d, "nv/linux.bootargs.cma", "cma=128M\n")
}

python do_env_append_phyboard-segin-imx6ul-7() {
    env_rm_rauc_nand_boot_scripts(d)
}

python do_env_append_phyboard-segin-imx6ul-8() {
    env_rm_rauc_nand_boot_scripts(d)
    env_add(d, "nv/linux.bootargs.cma", "cma=128M\n")
}

INTREE_DEFCONFIG = "imx_v7_defconfig"

COMPATIBLE_MACHINE  = "^("
COMPATIBLE_MACHINE .=  "phyflex-imx6-1"
COMPATIBLE_MACHINE .= "|phyflex-imx6-2"
COMPATIBLE_MACHINE .= "|phyflex-imx6-3"
COMPATIBLE_MACHINE .= "|phyflex-imx6-4"
COMPATIBLE_MACHINE .= "|phyflex-imx6-5"
COMPATIBLE_MACHINE .= "|phyflex-imx6-6"
COMPATIBLE_MACHINE .= "|phyflex-imx6-7"
COMPATIBLE_MACHINE .= "|phyflex-imx6-8"
COMPATIBLE_MACHINE .= "|phyflex-imx6-9"
COMPATIBLE_MACHINE .= "|phyflex-imx6-10"
COMPATIBLE_MACHINE .= "|phyflex-imx6-11"

COMPATIBLE_MACHINE .= "|phycard-imx6-2"

COMPATIBLE_MACHINE .= "|phyboard-mira-imx6-3"
COMPATIBLE_MACHINE .= "|phyboard-mira-imx6-4"
COMPATIBLE_MACHINE .= "|phyboard-mira-imx6-5"
COMPATIBLE_MACHINE .= "|phyboard-mira-imx6-6"
COMPATIBLE_MACHINE .= "|phyboard-mira-imx6-7"
COMPATIBLE_MACHINE .= "|phyboard-mira-imx6-8"
COMPATIBLE_MACHINE .= "|phyboard-mira-imx6-9"
COMPATIBLE_MACHINE .= "|phyboard-mira-imx6-10"
COMPATIBLE_MACHINE .= "|phyboard-mira-imx6-11"
COMPATIBLE_MACHINE .= "|phyboard-mira-imx6-12"
COMPATIBLE_MACHINE .= "|phyboard-mira-imx6-13"
COMPATIBLE_MACHINE .= "|phyboard-mira-imx6-14"
COMPATIBLE_MACHINE .= "|phyboard-mira-imx6-15"

COMPATIBLE_MACHINE .= "|phyboard-nunki-imx6-1"

COMPATIBLE_MACHINE .= "|phyboard-segin-imx6ul-2"
COMPATIBLE_MACHINE .= "|phyboard-segin-imx6ul-3"
COMPATIBLE_MACHINE .= "|phyboard-segin-imx6ul-4"
COMPATIBLE_MACHINE .= "|phyboard-segin-imx6ul-5"
COMPATIBLE_MACHINE .= "|phyboard-segin-imx6ul-6"
COMPATIBLE_MACHINE .= "|phyboard-segin-imx6ul-7"
COMPATIBLE_MACHINE .= "|phyboard-segin-imx6ul-8"
COMPATIBLE_MACHINE .= ")$"
