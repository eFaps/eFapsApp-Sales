//CHECKSTYLE:OFF
package org.efaps.esjp.ci;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsNoUpdate;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIMsgPhrase;

/**
 * This class is only used in case that the HumanResource App is not installed
 * to be able to compile the classes.
 * @author The eFaps Team
 */
@EFapsUUID("e8633952-4b53-4000-a66c-2ee62e99966e")
@EFapsApplication("eFapsApp-Sales")
@EFapsNoUpdate
public final class CIMsgHumanResource
{
    public static final _EmployeeWithNumberMsgPhrase EmployeeWithNumberMsgPhrase = new _EmployeeWithNumberMsgPhrase("c6686d34-f9d7-4bf4-b9f1-80dad440eac4");
    public static class _EmployeeWithNumberMsgPhrase extends CIMsgPhrase
    {
        protected _EmployeeWithNumberMsgPhrase(final String _uuid)
        {
            super(_uuid);
        }
    }
}
