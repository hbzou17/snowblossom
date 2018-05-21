package snowblossom;

import java.security.MessageDigest;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import snowblossom.proto.AddressSpec;
import snowblossom.proto.SigSpec;
import snowblossom.proto.WalletKeyPair;
import com.google.protobuf.ByteString;

import java.security.PublicKey;
import java.io.PrintStream;
import java.util.Set;
import com.google.common.collect.ImmutableSet;


public class AddressUtil
{
  public static AddressSpecHash getHashForSpec(AddressSpec spec)
  {
    return getHashForSpec(spec, DigestUtil.getMDAddressSpec());
  }

  public static AddressSpecHash getHashForSpec(AddressSpec spec, MessageDigest md)
  {
    try
    {
      ByteArrayOutputStream byte_out = new ByteArrayOutputStream(200);
      DataOutputStream data_out = new DataOutputStream(byte_out);

      data_out.writeInt(spec.getRequiredSigners());
      data_out.writeInt(spec.getSigSpecsCount());
      for(SigSpec sig_spec : spec.getSigSpecsList())
      {
        data_out.writeInt(sig_spec.getSignatureType());
        data_out.writeInt(sig_spec.getPublicKey().size());
        data_out.write(sig_spec.getPublicKey().toByteArray());
      }
      data_out.flush();
      md.update(byte_out.toByteArray());

      return new AddressSpecHash(md.digest());
    }
    catch(java.io.IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static AddressSpec getSimpleSpecForKey(ByteString key_data, int sig_type)
  {
   return AddressSpec.newBuilder()
    	.setRequiredSigners(1)
      .addSigSpecs( SigSpec.newBuilder()
        .setSignatureType( sig_type )
        .setPublicKey(key_data)
        .build())
      .build();
  }
  
  public static AddressSpec getSimpleSpecForKey(WalletKeyPair wkp)
  {
    return getSimpleSpecForKey(wkp.getPublicKey(), wkp.getSignatureType());
  }

  public static AddressSpec getSimpleSpecForKey(PublicKey key, int sig_type)
  {
    ByteString key_data;
    if (sig_type == SignatureUtil.SIG_TYPE_ECDSA_COMPRESSED)
    {
      key_data = KeyUtil.getCompressedPublicKeyEncoding(key);
    }
    else
    {
      key_data = ByteString.copyFrom(key.getEncoded());
    }
    return getSimpleSpecForKey(key_data, sig_type);
  }

  public static String getAddressString(String human, AddressSpecHash hash)
  {
    return Duck32.encode(human, hash.getBytes());
  }

  public static AddressSpecHash getHashForAddress(String human, String address)
    throws ValidationException
  {
    return new AddressSpecHash( Duck32.decode(human, address) );
  }

  public static void prettyDisplayAddressSpec(AddressSpec spec, PrintStream out, NetworkParams params)
    throws ValidationException
  {
    prettyDisplayAddressSpec(spec, out, params, 0, ImmutableSet.of());
  }
  public static void prettyDisplayAddressSpec(AddressSpec spec, PrintStream out, NetworkParams params, 
    int c_idx, Set<String> signed_list)
    throws ValidationException
  {
    AddressSpecHash hash = getHashForSpec(spec);
    String address = getAddressString(params.getAddressPrefix(), hash);
    out.print("AddressSpec " + address);

    out.println(String.format(" %dof%d", spec.getRequiredSigners(), spec.getSigSpecsCount()));

    for(int s = 0; s<spec.getSigSpecsCount(); s++)
    {
      String key = c_idx +":" + s;
      boolean signed=false;
      if (signed_list.contains(key)) signed=true;
      SigSpec sig = spec.getSigSpecs(s);

      String algo = SignatureUtil.getAlgo(sig.getSignatureType());

      out.print("   sigspec:" + algo);
      if (signed)
      {
        out.print(" SIGNED");
      }
      out.print(" pub:");
      out.println(HexUtil.getHexString(sig.getPublicKey()));
    }

  }

}
