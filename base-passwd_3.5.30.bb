SUMMARY = "Base system master password/group files"
DESCRIPTION = "The master copies of the user database files (/etc/passwd and /etc/group)."
SECTION = "base"
LICENSE = "CLOSED"

PR = "r1"

SRC_URI = "file://passwd \
           file://group \
           file://shadow \
          "

do_install() {
    install -d -m 755 ${D}${datadir}/base-passwd

    install -o root -g root -p -m 644 \
        ${WORKDIR}/passwd ${D}${datadir}/base-passwd/
    sed -i 's#:/root:#:${ROOT_HOME}:#' \
        ${D}${datadir}/base-passwd/passwd

    install -o root -g root -p -m 644 \
        ${WORKDIR}/group ${D}${datadir}/base-passwd/

    install -o root -g root -p -m 644 \
        ${WORKDIR}/shadow ${D}${datadir}/base-passwd/

    # Keep the original project behaviour for maintainer builds.
    if ${@bb.utils.contains('MAINTAINERS_MODE', 'y', 'true', 'false', d)}; then
        if ${@bb.utils.contains('ROOT_ENABLE', 'y', 'true', 'false', d)}; then
            sed -i 's#^root:\*:#root::#' \
                ${D}${datadir}/base-passwd/shadow
            sed -i '/^root/s#/bin/false#/bin/sh#' \
                ${D}${datadir}/base-passwd/passwd
            sed -i '/^Level15/s/^\(.*\)$/\1,root/' \
                ${D}${datadir}/base-passwd/group
        fi
    fi
}

# Yocto 2.5 uses recipe-specific sysroots.  Do not use the old
# SSTATEPOSTINSTFUNCS hook which attempted to read our own files through
# STAGING_DIR_TARGET while do_populate_sysroot was still being assembled.
# Instead install a sysroot postinst helper, following the Sumo-era
# base-passwd recipe model.
basepasswd_sysroot_postinst() {
#!/bin/sh

    install -d -m 755 ${STAGING_DIR_TARGET}${sysconfdir}

    for i in passwd group; do
        install -p -m 644 \
            ${STAGING_DIR_TARGET}${datadir}/base-passwd/\$i \
            ${STAGING_DIR_TARGET}${sysconfdir}/\$i
    done

    # Run user/group postinst fragments made available by useradd.bbclass.
    for script in ${STAGING_DIR_TARGET}${bindir}/postinst-useradd-*; do
        if [ -f \$script ]; then
            \$script
        fi
    done
}

SYSROOT_DIRS += "${sysconfdir}"
SYSROOT_PREPROCESS_FUNCS += "base_passwd_tweaksysroot"

base_passwd_tweaksysroot() {
    mkdir -p ${SYSROOT_DESTDIR}${bindir}
    dest=${SYSROOT_DESTDIR}${bindir}/postinst-${PN}
    echo "${basepasswd_sysroot_postinst}" > $dest
    chmod 0755 $dest
}

python populate_packages_prepend() {
    # Store the custom passwd/group/shadow contents directly in the package
    # preinst.  This preserves the original project's populate/ram/etc layout.
    f = open(d.expand("${STAGING_DATADIR}/base-passwd/passwd"), 'r')
    passwd = "".join(f.readlines())
    f.close()

    f = open(d.expand("${STAGING_DATADIR}/base-passwd/group"), 'r')
    group = "".join(f.readlines())
    f.close()

    f = open(d.expand("${STAGING_DATADIR}/base-passwd/shadow"), 'r')
    shadow = "".join(f.readlines())
    f.close()

    # The generated preinst uses an unquoted here-doc, so keep password-hash
    # dollar signs literal when the shell executes it.
    shadow = shadow.replace("$", "\\$")

    preinst = """#!/bin/sh
mkdir -p $D${sysconfdir}
mkdir -p $D${sysconfdir}/populate/ram/etc
if [ ! -e $D${sysconfdir}/populate/ram/etc/passwd ]; then
\tcat << EOF > $D${sysconfdir}/populate/ram/etc/passwd
""" + passwd + """EOF
fi
if [ ! -e $D${sysconfdir}/populate/ram/etc/shadow ]; then
\tcat << EOF > $D${sysconfdir}/populate/ram/etc/shadow
""" + shadow + """EOF
fi
if [ ! -e $D${sysconfdir}/populate/ram/etc/group ]; then
\tcat << EOF > $D${sysconfdir}/populate/ram/etc/group
""" + group + """EOF
fi
"""

    d.setVar(d.expand('pkg_preinst_${PN}'), preinst)
}

# populate_packages_prepend reads the staged copies above.
addtask do_package after do_populate_sysroot

ALLOW_EMPTY_${PN} = "1"

PACKAGES =+ "${PN}-update"
FILES_${PN}-update = "${sbindir}/* ${datadir}/${PN}"
