package uk.ac.cam.seh208.middleware;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import uk.ac.cam.seh208.middleware.common.Persistence;

import static junit.framework.Assert.assertEquals;


/**
 * Instrumented test for the correctness of parceling Persistence enums.
 */
@RunWith(Parameterized.class)
public class PersistenceTest {
    @Parameterized.Parameters
    public static Collection<Persistence> parameters() {
        return Arrays.asList(Persistence.values());
    }

    @Parameterized.Parameter
    public Persistence value;

    @Test
    public void testParcel() {
        Parcel parcel = Parcel.obtain();

        // Write the value to a parcel, and then test that the
        // unparceled value matches the original value.
        value.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        assertEquals(value, Persistence.CREATOR.createFromParcel(parcel));
    }
}
