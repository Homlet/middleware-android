package uk.ac.cam.seh208.middleware.core.control;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.Update;
import android.content.Context;
import android.util.Log;

import java.util.List;

import uk.ac.cam.seh208.middleware.common.EndpointDetails;
import uk.ac.cam.seh208.middleware.common.Persistence;
import uk.ac.cam.seh208.middleware.common.exception.BadHostException;
import uk.ac.cam.seh208.middleware.common.exception.BadQueryException;
import uk.ac.cam.seh208.middleware.common.exception.BadSchemaException;
import uk.ac.cam.seh208.middleware.common.exception.EndpointCollisionException;
import uk.ac.cam.seh208.middleware.common.exception.ProtocolException;
import uk.ac.cam.seh208.middleware.core.MiddlewareService;


@Database(version = 1, entities = { EndpointData.class, MappingData.class })
public abstract class MiddlewareDatabase extends RoomDatabase {

    @Dao
    protected interface EndpointDAO {
        @Query("SELECT * FROM endpoints WHERE name = :name")
        EndpointData getEndpoint(String name);

        @Query("SELECT * FROM endpoints")
        List<EndpointData> getEndpoints();

        @Query("SELECT * FROM mappings WHERE endpointName = :endpointName")
        List<MappingData> getMappings(String endpointName);

        @Insert
        void insertEndpoint(EndpointData endpoint);

        @Insert
        void insertMapping(MappingData mapping);

        @Update
        void updateEndpoint(EndpointData endpoint);

        @Query("DELETE FROM endpoints WHERE name = :name")
        void deleteEndpoint(String name);

        @Query("DELETE FROM mappings WHERE mappingId = :mappingId")
        void deleteMapping(long mappingId);
    }


    private static MiddlewareDatabase INSTANCE;

    public static MiddlewareDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                            MiddlewareDatabase.class,
                                            "middleware-database")
                    .allowMainThreadQueries()
                    .build();
        }

        return INSTANCE;
    }


    abstract EndpointDAO endpointDAO();


    public void restore(MiddlewareService service) {
        EndpointDAO dao = endpointDAO();

        for (EndpointData endpointData : dao.getEndpoints()) {
            try {
                // Recreate the stored endpoint in the new middleware instance.
                service.createEndpoint(
                        new EndpointDetails(
                                endpointData.getName(),
                                endpointData.getDesc(),
                                endpointData.getPolarity(),
                                endpointData.getSchema(),
                                endpointData.getTags()),
                        endpointData.isExposed(),
                        endpointData.isForceable(),
                        false);

                Endpoint endpoint = service
                        .getEndpointSet()
                        .getEndpointByName(endpointData.getName());

                for (MappingData mappingData : dao.getMappings(endpoint.getName())) {
                    // Map from the endpoint, using the original query and persistence policy.
                    endpoint.map(mappingData.getQuery(), mappingData.getPersistence());

                    // Delete the old mapping.
                    dao.deleteMapping(mappingData.getMappingId());
                }
            } catch (EndpointCollisionException | BadSchemaException | BadQueryException
                                                | BadHostException | ProtocolException e) {
                Log.w(getTag(), "Error restoring middleware state:", e);
            }
        }
    }

    public void insertEndpoint(EndpointDetails details, boolean exposed, boolean forceable) {
        endpointDAO().insertEndpoint(new EndpointData(details, exposed, forceable));
    }

    public void insertMapping(Mapping mapping) {
        endpointDAO().insertMapping(new MappingData(
                mapping.getMappingId(),
                mapping.getEndpointName(),
                mapping.getQuery(),
                mapping.getPersistence()));
    }

    public void setEndpointExposed(String name, boolean exposed) {
        EndpointDAO dao = endpointDAO();

        // Get the endpoint data object.
        EndpointData endpoint = dao.getEndpoint(name);

        // Update the endpoint data object.
        endpoint.setExposed(exposed);

        // Commit the data object back to the database.
        dao.updateEndpoint(endpoint);
    }

    public void setEndpointForceable(String name, boolean forceable) {
        EndpointDAO dao = endpointDAO();

        // Get the endpoint data object.
        EndpointData endpoint = dao.getEndpoint(name);

        // Update the endpoint data object.
        endpoint.setForceable(forceable);

        // Commit the data object back to the database.
        dao.updateEndpoint(endpoint);
    }

    public void deleteEndpoint(String name) {
        endpointDAO().deleteEndpoint(name);
    }

    public void deleteMapping(long mappingId) {
        endpointDAO().deleteMapping(mappingId);
    }


    public static String getTag() {
        return "MW_DATABASE";
    }
}
