SUMMARY = "Harpy thermal daemon and library"
DESCRIPTION = "Harpy thermal daemon and library"
SECTION = "base"
LICENSE = "CLOSED"

require harpy.inc

inherit cmake update-rc.d

# libcli - for Harpy CMake module
# libsystem - for nlog.h
DEPENDS = "libcli libsystem libconfig"

S = "${HS}/thermald"
EXTERNALSRC = '${@oe.utils.conditional("HSRC", "", "", "${S}", d)}'

SRC_URI += " \
    file://init_thermald \
    file://thermald.conf \
    file://set_const_speed \
"

EXTRA_OECMAKE += '${@bb.utils.contains("MAINTAINERS_MODE", "y", "-DBUILD_TESTING=On", "", d)}'

INITSCRIPT_NAME = "thermald"
INITSCRIPT_PARAMS = "start 22 5 . stop 51 0 4 6 ."

do_install_append() {
    install -d ${D}${sysconfdir}/init.d

    install -m 0755 ${WORKDIR}/init_thermald \
        ${D}${sysconfdir}/init.d/${INITSCRIPT_NAME}

    install -m 0644 ${WORKDIR}/thermald.conf \
        ${D}${sysconfdir}/

    install -m 0755 ${WORKDIR}/set_const_speed \
        ${D}${bindir}/
}

do_unpack[nostamp] = "1"
do_install[nostamp] = "1"
