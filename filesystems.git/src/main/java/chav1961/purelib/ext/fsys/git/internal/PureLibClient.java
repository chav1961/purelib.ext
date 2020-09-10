package chav1961.purelib.ext.fsys.git.internal;

import java.net.URI;

import chav1961.purelib.basic.PureLibSettings;
import chav1961.purelib.basic.exceptions.LocalizationException;
import chav1961.purelib.basic.exceptions.PreparationException;
import chav1961.purelib.i18n.LocalizerFactory;
import chav1961.purelib.i18n.interfaces.Localizer;

public class PureLibClient {
	static {
		try{PureLibSettings.PURELIB_LOCALIZER.add(LocalizerFactory.getLocalizer(URI.create(Localizer.LOCALIZER_SCHEME+":xml:root://chav1961.purelib.ext.fsys.git.internal.PureLibClient/i18n/localization.xml")));
		} catch (LocalizationException e) {
			throw new PreparationException("Registration of localizer in module ["+PureLibClient.class.getModule().getName()+"] failed: "+e.getLocalizedMessage(),e);
		}
	}
	
	public static void registerInPureLib() {
	}
}
