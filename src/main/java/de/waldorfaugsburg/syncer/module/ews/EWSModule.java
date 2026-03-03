package de.waldorfaugsburg.syncer.module.ews;

import com.microsoft.aad.msal4j.*;
import de.waldorfaugsburg.syncer.SyncerApplication;
import de.waldorfaugsburg.syncer.module.AbstractModule;
import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.misc.ConnectingIdType;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.MapiPropertyType;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.Contact;
import microsoft.exchange.webservices.data.misc.ImpersonatedUserId;
import microsoft.exchange.webservices.data.property.complex.FolderId;
import microsoft.exchange.webservices.data.property.definition.ExtendedPropertyDefinition;
import microsoft.exchange.webservices.data.search.ItemView;
import microsoft.exchange.webservices.data.search.filter.SearchFilter;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class EWSModule extends AbstractModule {

    private static final String EWS_URL = "https://outlook.office365.com/EWS/Exchange.asmx";
    private static final String EWS_SCOPE = "https://outlook.office365.com/.default";
    private static final String EWS_AUTHORITY = "https://login.microsoftonline.com/%s/";
    private static final ExtendedPropertyDefinition PROCURAT_ID_PROPERTY;

    static {
        try {
            PROCURAT_ID_PROPERTY = new ExtendedPropertyDefinition(
                    UUID.fromString("757f160d-68cf-4dbb-8c5f-feab33b86145"),
                    "ProcuratId",
                    MapiPropertyType.Integer);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private EWSConfig config;
    private ExchangeService service;
    private Folder contactFolder;

    public EWSModule(final SyncerApplication application) {
        super(application);
    }

    @Override
    public void init() throws Exception {
        config = getApplication().loadConfiguration("ews.json", EWSConfig.class);

        final IClientCredential credential = ClientCredentialFactory.createFromSecret(config.getClientSecret());
        final ConfidentialClientApplication confidentialClientApplication = ConfidentialClientApplication
                .builder(config.getClientId(), credential)
                .authority(String.format(EWS_AUTHORITY, config.getTenantId()))
                .build();
        final ClientCredentialParameters parameters = ClientCredentialParameters.builder(Set.of(EWS_SCOPE)).build();
        final IAuthenticationResult result = confidentialClientApplication.acquireToken(parameters).join();

        service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
        service.getHttpHeaders().put("Authorization", "Bearer " + result.accessToken());
        service.setUrl(new URI(EWS_URL));
        service.setImpersonatedUserId(new ImpersonatedUserId(ConnectingIdType.SmtpAddress, config.getImpersonatedUserId()));

        contactFolder = service.bindToFolder(FolderId.getFolderIdFromString(config.getContactFolderId()), PropertySet.IdOnly);
    }

    @Override
    public void destroy() throws Exception {
        service.close();
    }

    public Contact findContactByPersonId(final int personId) {
        try {
            return (Contact) contactFolder.findItems(new SearchFilter.IsEqualTo(PROCURAT_ID_PROPERTY, personId), new ItemView(1)).getItems().getFirst();
        } catch (final Exception e) {
            log.error("Error finding contact by person id {}", personId);
            return null;
        }
    }
}
