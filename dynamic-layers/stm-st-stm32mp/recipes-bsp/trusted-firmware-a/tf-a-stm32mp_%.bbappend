FILESEXTRAPATHS_prepend := "${THISDIR}/${PN}:"

SRC_URI += " \
	file://0002-phycore-update.patch \
        file://0003-fix-emmc-pinmux-phycore-usage.patch \
	file://0004-fix-internal-eth-clk-pll4.patch \
	file://0005-tf-a-phycore-stm32mp1xx-alpha2.patch \
"
