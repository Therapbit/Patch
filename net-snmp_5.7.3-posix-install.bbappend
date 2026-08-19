# Yocto 2.5.1 migration fix for net-snmp 5.7.3
#
# The original recipe used the bash-only substitution:
#   ${SVDIRs//\// }
# BitBake runs shell tasks through /bin/sh, so dash reports
# "Bad substitution".  Redefine install_initscript() using only
# shell syntax accepted by /bin/sh.

install_initscript() {
    SVDIRs='populate/ram/etc/sv/snmpd'
    SVDIR="${D}${sysconfdir}"
    OLDIFS="${IFS}"

    IFS='/'
    for d in ${SVDIRs}; do
        [ -n "${d}" ] || continue
        SVDIR="${SVDIR}/${d}"
        [ -d "${SVDIR}" ] || install -d "${SVDIR}"
    done
    IFS="${OLDIFS}"

    touch "${SVDIR}/down"
    install -m 0744 "${WORKDIR}/ri_init" "${SVDIR}/run"

    sed -e 's/^### \{0,1\}//g' > "${D}${sysconfdir}/init.d/snmpd" <<EOF
### #!/bin/sh
### export SVDIR=/etc/service
### start() {
###     sv u snmpd
### }
### stop() {
###     sv d snmpd
### }
### \$1
EOF

    chmod 0744 "${D}${sysconfdir}/init.d/snmpd"
}
