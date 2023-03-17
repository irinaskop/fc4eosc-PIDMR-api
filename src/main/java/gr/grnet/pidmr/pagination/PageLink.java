package gr.grnet.pidmr.pagination;

import lombok.Builder;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Builder
@Schema(name="PageLink", description="An object represents the links of paginated entities.")
public class PageLink {

    @Schema(
            type = SchemaType.STRING,
            implementation = String.class,
            description = "Uri to paginated entities.",
            example = "http://pid.metaresolver.api/v1/providers?page=1&size=10"
    )
    public String href;

    @Schema(
            type = SchemaType.STRING,
            implementation = String.class,
            description = "Descriptor for how the target resource relates to the current resource.",
            example = "first"
    )
    public String rel;
}
