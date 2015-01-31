package owltools2;

import java.util.ListIterator;

import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.ParseException;

// See http://stackoverflow.com/a/8613949
public class ExtendedPosixParser extends PosixParser {

    private boolean ignoreUnrecognizedOption;

    public ExtendedPosixParser(final boolean ignoreUnrecognizedOption) {
        this.ignoreUnrecognizedOption = ignoreUnrecognizedOption;
    }

    @Override
    protected void processOption(final String arg, final ListIterator iter)
        throws ParseException {
      boolean hasOption = getOptions().hasOption(arg);

      if (hasOption || !ignoreUnrecognizedOption) {
          super.processOption(arg, iter);
      }
    }

}
