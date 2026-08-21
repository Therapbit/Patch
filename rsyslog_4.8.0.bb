SUMMARY = "Rsyslog is an enhanced multi-threaded syslogd"
DESCRIPTION = "\
Rsyslog is an enhanced syslogd supporting, among others, MySQL,\
PostgreSQL, failover log destinations, syslog/tcp, fine grain\
output format control, high precision timestamps, queued operations\
and the ability to filter on any message part. It is quite\
compatible to stock sysklogd and can be used as a drop-in replacement.\
Its advanced features make it suitable for enterprise-class,\
encryption protected syslog relay chains while at the same time being\
very easy to setup for the novice user."

DEPENDS = "zlib libestr bison-native flex-native"

HOMEPAGE = "http://www.rsyslog.com/"
LICENSE = "GPLv3 & LGPLv3"
LIC_FILES_CHKSUM = "file://COPYING;md5=51d9635e646fb75e1b74c074f788e973 \
                    file://COPYING.LESSER;md5=cb7903f1e5c39ee838209e130dca270a"

SRC_URI = "https://github.com/rsyslog/rsyslog/archive/v${PV}.tar.gz;downloadfilename=${PN}-${PV}.tar.gz \
           file://initscript \
           file://rsyslog.conf \
           file://rsyslog-msgstart-del.patch \
           file://rsyslog_init \
"
SRC_URI += "${@bb.utils.contains('DISTRO_FEATURES', 'opklog', 'file://opkLog.conf', '', d)}"

SRC_URI[md5sum] = "9d968ca6a1e8ef516af2d77aa8e142b9"
SRC_URI[sha256sum] = "b7109c2ab56c64896589fe658ef5befbbfbbb6acfb7b0395346f7aa2153179a27707a"

inherit autotools pkgconfig systemd update-rc.d update-alternatives

#EXTRA_OECONF += "--enable-cached-man-pages"

INITSCRIPT_NAME = "rsyslogd"
INITSCRIPT_PARAMS = "start 20 5 2 . stop 70 0 1 6 ."

PACKAGECONFIG = "\
    rsyslogd rsyslogrt klog inet regexp omuxsock \
"

# default yes in configure
PACKAGECONFIG[zlib] = "--enable-zlib,--disable-zlib,zlib,"
PACKAGECONFIG[rsyslogd] = "--enable-rsyslogd,--disable-rsyslogd,,"
PACKAGECONFIG[rsyslogrt] = "--enable-rsyslogrt,--disable-rsyslogrt,,"
PACKAGECONFIG[inet] = "--enable-inet,--disable-inet,,"
PACKAGECONFIG[klog] = "--enable-klog,--disable-klog,,"
PACKAGECONFIG[regexp] = "--enable-regexp,--disable-regexp,,"
PACKAGECONFIG[testbench] = "--enable-testbench,--disable-testbench,,"

# default no in configure
PACKAGECONFIG[debug] = "--enable-debug,--disable-debug,,"
PACKAGECONFIG[imdiag] = "--enable-imdiag,--disable-imdiag,,"
PACKAGECONFIG[snmp] = "--enable-snmp,--disable-snmp,net-snmp,"
PACKAGECONFIG[gnutls] = "--enable-gnutls,--disable-gnutls,gnutls,"
PACKAGECONFIG[mysql] = "--enable-mysql,--disable-mysql,mysql5,"
PACKAGECONFIG[postgresql] = "--enable-pgsql,--disable-pgsql,postgresql,"
PACKAGECONFIG[libdbi] = "--enable-libdbi,--disable-libdbi,libdbi,"
PACKAGECONFIG[mail] = "--enable-mail,--disable-mail,,"
PACKAGECONFIG[omuxsock] = "--enable-omuxsock,--disable-omuxsock,,"

do_install_append() {
    install -d ${D}${sysconfdir}/init.d
    install -d ${D}/usr/bin
    install -d ${D}${sysconfdir}/syslog
    install -d ${D}${sysconfdir}/populate/ram/etc/
    install -d ${D}${sysconfdir}/populate/ram/etc/rsyslog.d/

    if ${@bb.utils.contains('DISTRO_FEATURES', 'opklog', 'true', 'false', d)}; then
        sed -e '/^$/d' -e '/^#/d' ${WORKDIR}/opkLog.conf > ${D}${sysconfdir}/populate/ram/etc/rsyslog.d/opkLog.conf
    fi

    install -m 0644 ${WORKDIR}/rsyslog.conf ${D}${sysconfdir}/

    ln -sf /mnt/ram/etc/rsyslog.d ${D}${sysconfdir}/rsyslog.d

    # Use update-rc.d for SysV init scripts.  Do not manage /etc/init.d/*
    # through update-alternatives: Sumo rejects that during do_package.
    install -m 0755 ${WORKDIR}/rsyslog_init ${D}${sysconfdir}/init.d/${INITSCRIPT_NAME}
}

FILES_${PN} = "${sysconfdir} ${bindir} ${sbindir} "
FILES_${PN} += "${libdir}/rsyslog/lmnet.so "
FILES_${PN} += "${libdir}/rsyslog/lmuxsock.so "
FILES_${PN} += "${libdir}/rsyslog/omuxsock.so "
FILES_${PN} += "${libdir}/rsyslog/lmregexp.so "
FILES_${PN} += "${libdir}/rsyslog/imklog.so"

# higher than sysklogd's 100
ALTERNATIVE_PRIORITY = "110"
ALTERNATIVE_${PN} = "syslogd syslog-conf"

ALTERNATIVE_LINK_NAME[syslogd] = "${base_sbindir}/syslogd"
ALTERNATIVE_TARGET[syslogd] = "${sbindir}/rsyslogd"
ALTERNATIVE_LINK_NAME[syslog-conf] = "${sysconfdir}/syslog.conf"
ALTERNATIVE_TARGET[syslog-conf] = "${sysconfdir}/rsyslog.conf"

CONFFILES_${PN} = "${sysconfdir}/rsyslog.conf"

RPROVIDES_${PN} += "${PN}-systemd"
RREPLACES_${PN} += "${PN}-systemd"
RCONFLICTS_${PN} += "${PN}-systemd"
SYSTEMD_SERVICE_${PN} = "${BPN}.service"
