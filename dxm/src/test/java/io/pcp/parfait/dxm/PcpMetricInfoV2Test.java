package io.pcp.parfait.dxm;

import io.pcp.parfait.dxm.PcpMetricInfoV2.MetricInfoStoreV2;
import io.pcp.parfait.dxm.PcpString.PcpStringStore;
import io.pcp.parfait.dxm.semantics.Semantics;
import io.pcp.parfait.dxm.types.MmvMetricType;
import io.pcp.parfait.dxm.types.TypeHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;

public class PcpMetricInfoV2Test {

    private static final int STRING_OFFSET = 10;
    private static final int METRIC_TYPE_IDENTIFIER = 909;
    private static final int SHORT_HELP_TEXT_OFFSET = 11;
    private static final int LONG_HELP_TEXT_OFFSET = 12;
    private static final int EXPECTED_PCP_METRIC_INFO_SIZE = 48;
    private static final int SEMANTICS_VALUE = 32;
    private static final int THIS_PCP_METRIC_INFO_OFFSET = 400;

    private PcpMetricInfoV2 pcpMetricInfoV2;

    @Mock
    private PcpString nameAsPcpString;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        pcpMetricInfoV2 = new PcpMetricInfoV2("name", 1, nameAsPcpString);
    }

    @Test
    public void writeToMmvWriteToTheByteBuffer() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(48);
        TypeHandler<Object> typeHandler = mock(TypeHandler.class);
        MmvMetricType mmvMetricType = mock(MmvMetricType.class);
        PcpString shortHelpPcpString = mock(PcpString.class);
        PcpString longHelpPcpString = mock(PcpString.class);
        Semantics semantics = mock(Semantics.class);


        when(nameAsPcpString.getOffset()).thenReturn(STRING_OFFSET);
        when(typeHandler.getMetricType()).thenReturn(mmvMetricType);
        when(mmvMetricType.getIdentifier()).thenReturn(METRIC_TYPE_IDENTIFIER);
        when(shortHelpPcpString.getOffset()).thenReturn(SHORT_HELP_TEXT_OFFSET);
        when(longHelpPcpString.getOffset()).thenReturn(LONG_HELP_TEXT_OFFSET);
        when(semantics.getPcpValue()).thenReturn(SEMANTICS_VALUE);

        pcpMetricInfoV2.setTypeHandler(typeHandler);
        pcpMetricInfoV2.setHelpText(shortHelpPcpString, longHelpPcpString);
        pcpMetricInfoV2.setSemantics(semantics);

        pcpMetricInfoV2.writeToMmv(byteBuffer);

        byte[] expectedBytes = {
            0,  0,  0,  0,  0,  0,  0,  10,
            0,  0,  0,  1,  0,  0,  3,  -115,
            0,  0,  0,  32,  0,  0,  0,  0,
            -1, -1, -1, -1, 0,  0,  0,  0,
            0,  0,  0,  0,  0,  0,  0,  11,
            0,  0,  0,  0,  0,  0,  0,  12,
        };

        assertArrayEquals(expectedBytes, byteBuffer.array());
    }

    @Test
    public void writeToMmvShouldSetThePositionOfTheBufferBeforeWriting() {
        ByteBuffer byteBuffer = mock(ByteBuffer.class);
        TypeHandler<Object> typeHandler = mock(TypeHandler.class);

        when(typeHandler.getMetricType()).thenReturn(mock(MmvMetricType.class));

        pcpMetricInfoV2.setOffset(THIS_PCP_METRIC_INFO_OFFSET);
        pcpMetricInfoV2.setTypeHandler(typeHandler);

        pcpMetricInfoV2.writeToMmv(byteBuffer);

        InOrder inOrder = Mockito.inOrder(byteBuffer);

        inOrder.verify(byteBuffer).position(THIS_PCP_METRIC_INFO_OFFSET);
        inOrder.verify(byteBuffer).putLong(anyLong());
    }

    @Test
    public void byteSizeShouldReturnTheByteSizeOfTheMmvFormat() {
        assertEquals(EXPECTED_PCP_METRIC_INFO_SIZE, pcpMetricInfoV2.byteSize());
    }

    @Test
    public void metricInfoStoreShouldCreateANewPcpMetricInfoV2() {
        IdentifierSourceSet identifierSourceSet = mock(IdentifierSourceSet.class);
        PcpStringStore stringStore = mock(PcpStringStore.class);
        PcpString pcpString = mock(PcpString.class);
        IdentifierSource identifierSource = mock(IdentifierSource.class);

        when(identifierSourceSet.metricSource()).thenReturn(identifierSource);
        when(identifierSource.calculateId(eq("my.metric"), ArgumentMatchers.<Integer>anySet())).thenReturn(123);
        when(stringStore.createPcpString("my.metric")).thenReturn(pcpString);

        MetricInfoStoreV2 metricInfoStoreV2 = new MetricInfoStoreV2(identifierSourceSet, stringStore);

        PcpMetricInfo actual = metricInfoStoreV2.byName("my.metric");

        PcpMetricInfoV2 expected = new PcpMetricInfoV2("my.metric", 123, pcpString);

        assertReflectionEquals(expected, actual);
    }
}